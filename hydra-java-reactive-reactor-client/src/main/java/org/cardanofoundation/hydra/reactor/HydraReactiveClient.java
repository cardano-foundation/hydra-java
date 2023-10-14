package org.cardanofoundation.hydra.reactor;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.Request;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.*;
import org.cardanofoundation.hydra.core.store.UTxOStore;
import org.cardanofoundation.hydra.reactor.commands.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static org.cardanofoundation.hydra.client.HydraClientOptions.TransactionFormat.CBOR;
import static org.cardanofoundation.hydra.core.model.HydraState.*;
import static org.cardanofoundation.hydra.core.model.Tag.FanoutTx;
import static org.cardanofoundation.hydra.core.utils.HexUtils.encodeHexString;

@Slf4j
public class HydraReactiveClient extends HydraQueryEventListener.Stub {

    public static final Duration DEF_TIMEOUT_DURATION = Duration.ofMinutes(1);
    @Nullable private HydraWSClient hydraWSClient;

    private final HydraClientOptions hydraClientOptions;

    private final Duration timeout;

    public HydraReactiveClient(UTxOStore uTxOStore,
                               String baseUrl) {
        this(uTxOStore, baseUrl, DEF_TIMEOUT_DURATION);
    }
    public HydraReactiveClient(UTxOStore uTxOStore,
                               String baseUrl,
                               Duration timeout) {
        this.timeout = timeout;

        this.hydraClientOptions = HydraClientOptions.builder(baseUrl)
                .utxoStore(uTxOStore)
                .transactionFormat(CBOR)
                .history(false)
                .snapshotUtxo(true)
                .build();
    }

    private Map<String, MonoSink> monoSinkMap = new ConcurrentHashMap<>();

    private void initWSClient() {
        if (this.hydraWSClient == null) {
            this.hydraWSClient = new HydraWSClient(hydraClientOptions);
            hydraWSClient.addHydraQueryEventListener(this);
            this.monoSinkMap = new ConcurrentHashMap<>();
        }
    }

    public Flux<BiHydraState> getHydraStatesStream() {
        if (hydraWSClient == null) {
            return Flux.empty();
        }

        var adapter = new FluxSinkHydraStateAdapter();

        // TODO when we close the connection how do we terminate the flux here?
        return Flux.<BiHydraState>create(fluxSink -> {
            adapter.setSink(fluxSink);
            hydraWSClient.addHydraStateEventListener((adapter));
        }).doFinally(signal -> {
            log.debug("Removing hydra state event listener...");
            hydraWSClient.removeHydraStateEventListener(adapter);
        });
    }

    public Flux<Response> getHydraResponsesStream() {
        if (hydraWSClient == null) {
            return Flux.empty();
        }

        var adapter = new FluxSinkResponseAdapter();
        return Flux.<Response>create(fluxSink -> {
            adapter.setSink(fluxSink);
            hydraWSClient.addHydraQueryEventListener(adapter);
        }).doFinally(signal -> {
            log.debug("Removing hydra query event listener...");
            hydraWSClient.removeHydraQueryEventListener(adapter);
        });
    }

    private void destroyWSClient() {
        if (hydraWSClient != null) {
            hydraWSClient.clearAllHydraQueryEventListeners();
            hydraWSClient.clearAllHydraStateEventListeners();
        }
        this.monoSinkMap = new ConcurrentHashMap<>();

        this.hydraWSClient = null;
    }

    @Override
    public void onResponse(Response response) {
        log.debug("Tag:{}, seq:{}", response.getTag(), response.getSeq());

        if (response instanceof GreetingsResponse gr) {
            var utxo = gr.getSnapshotUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);

            applyMonoSuccess(ConnectCommand.key(), gr);
        }

        if (response instanceof HeadIsOpenResponse ho) {
            // we get initial UTxOs here as well
            var utxoMap = ho.getUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxoMap);
        }

        if (response instanceof CommittedResponse) {
            CommittedResponse cr = (CommittedResponse) response;
            var utxoMap = cr.getUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxoMap);

            applyMonoSuccess(CommittedCommand.key(), cr);
        }

        if (response instanceof HeadIsClosedResponse hc) {
            applyMonoSuccess(CloseHeadCommand.key(), hc);
        }

        if (response instanceof SnapshotConfirmed sc) {
            Map<String, UTXO> utxo = sc.getSnapshot().getUtxo();

            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);

            for (var txId : sc.getSnapshot().getConfirmedTransactions()) {
                var txResult = new TxResult(txId, true); // TODO - is this correct to assume trx will be valid???

                // get isValid from MonoSinkWrapper
                TxSubmitGlobalCommand txSubmitGlobalCommand = TxSubmitGlobalCommand.of(txId, TODO);

                applyMonoSuccess(txSubmitGlobalCommand.key(), txResult);
            }
        }

        if (response instanceof HeadIsInitializingResponse hi) {
            applyMonoSuccess(InitHeadCommand.key(), hi);
        }

        if (response instanceof HeadIsAbortedResponse ha) {
            applyMonoSuccess(AbortHeadCommand.key(), ha);
        }

        if (response instanceof HeadIsFinalizedResponse hf) {
            applyMonoSuccess(FanOutHeadCommand.key(), hf);
        }

        if (response instanceof PostTxOnChainFailedResponse failedResponse) {
            if (failedResponse.getPostChainTx().getTag() == FanoutTx) {
                applyMonoError(FanOutHeadCommand.key(), "Fanout failed.");
            }
        }

        if (response instanceof TxValidResponse txResponse) {
            String txId = txResponse.getTransaction().getId();
            TxResult txResult = new TxResult(txId, true);

            applyMonoSuccess(TxSubmitLocalCommand.of(txId).toString(), txResult);
        }
        if (response instanceof TxInvalidResponse txResponse) {
            String txId = txResponse.getTransaction().getId();
            String reason = txResponse.getValidationError().getReason();

            TxResult txResult = new TxResult(txId, false, reason);

            applyMonoSuccess(TxSubmitLocalCommand.of(txId).key(), txResult);
            //applyMonoSuccess(TxSubmitGlobalCommand.of(txId).key(), txResult);
        }

        if (response instanceof GetUTxOResponse getUTxOResponse) {
            applyMonoSuccess(GetUTxOCommand.key(), getUTxOResponse);
        }
    }

    public HydraState getHydraState() {
        if (hydraWSClient == null) {
            return Unknown;
        }

        return hydraWSClient.getHydraState();
    }

    public Mono<GetUTxOResponse> getUTxOs() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra head is not open yet!");

            return Mono.empty();
        }

        var sinkM = getMonoSink(GetUTxOCommand.key());

        if (sinkM.isPresent()) {
            log.warn("getUTxO request already in progress...");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(GetUTxOCommand.key(), monoSink);
            hydraWSClient.getUTXO();
        });
    }

    public Mono<TxResult> submitTx(String txId, byte[] txCbor) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra head is not open yet!");

            return Mono.empty();
        }

        var command = TxSubmitLocalCommand.of(txId);

        return Mono.<TxResult>create(monoSink -> {
                    storeMonoSinkReference(command.key(), monoSink);
                    hydraWSClient.submitTx(encodeHexString(txCbor));
                })
                .timeout(timeout, Mono.defer(() -> {
                    applyMonoCleanup(command.key());

                    return Mono.error(new TimeoutException("tx submit timeout, txId: " + txId));
                }));
    }

    public Mono<TxResult> submitTxFullConfirmation(String txId,
                                                   byte[] txCbor) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra head is not open yet!");

            return Mono.empty();
        }

        var command = TxSubmitGlobalCommand.of(txId);

        var localTxMono = submitTx(txId, txCbor);

        return localTxMono
                .flatMap(localTxResult -> {
                    var isTxValid = localTxResult.isValid();

                    return Mono.<TxResult>create(monoSink -> {
                        storeMonoSinkReference(command.key(), monoSink);
                        hydraWSClient.submitTx(encodeHexString(txCbor));
                    })
                    .timeout(timeout, Mono.defer(() -> {
                        applyMonoCleanup(command.key());

                        return Mono.error(new TimeoutException("tx submit timeout, txId: " + txId));
                    }));
                });
    }

    protected void storeMonoSinkReference(String key, MonoSink monoSink) {
        monoSinkMap.put(key, monoSink);
    }

    protected Optional<MonoSink> getMonoSink(String key) {
        return Optional.ofNullable(monoSinkMap.get(key));
    }

    protected <T extends Request> void applyMonoSuccess(String key, Object result) {
        var monoSink = monoSinkMap.remove(key);
        if (monoSink == null) {
            return;
        }

        monoSink.success(result);
    }

    protected <T extends Request> void applyMonoSuccess(String key) {
        var monoSink = monoSinkMap.remove(key);

        monoSink.success();
    }

    protected <T extends Request> void applyMonoError(String key, Object result) {
        var monoSink = monoSinkMap.remove(key);

        monoSink.error(new HydraException(String.valueOf(result)));
    }

    protected  void applyMonoCleanup(String key) {
        monoSinkMap.remove(key);
    }

    public Mono<GreetingsResponse> openConnection() {
        initWSClient();

        assert hydraWSClient != null;

        if (hydraWSClient.isOpen()) {
            log.warn("Connection already open!");

            return Mono.empty();
        }

        var sinkM = getMonoSink(ConnectCommand.key());

        if (sinkM.isPresent()) {
            log.warn("connect request already in progress...");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(ConnectCommand.key(), monoSink);
            hydraWSClient.connect();
        });
    }

    public Mono<Boolean> closeConnection() throws InterruptedException {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        hydraWSClient.closeBlocking();
        destroyWSClient();

        return Mono.just(true);
    }

    public Mono<HeadIsAbortedResponse> abortHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Initializing) {
            log.warn("Hydra not in initializing state...");

            return Mono.empty();
        }

        var sinkM = getMonoSink(AbortHeadCommand.key());

        if (sinkM.isPresent()) {
            log.warn("abortHead request already in progress...");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(AbortHeadCommand.key(), monoSink);
            hydraWSClient.abort();
        });
    }

    public Mono<HeadIsInitializingResponse> initHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Idle && hydraWSClient.getHydraState() != Final) {
            log.warn("Hydra needs to be either in Idle or Final state!");

            return Mono.empty();
        }

        var sinkM = getMonoSink(InitHeadCommand.key());

        if (sinkM.isPresent()) {
            log.warn("init head request already in progress!");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(InitHeadCommand.key(), monoSink);
            hydraWSClient.init();
        });
    }

    public Mono<CommittedResponse> commitEmptyToTheHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Initializing) {
            log.warn("Hydra needs to be in initialising state!");

            return Mono.empty();
        }

        var sinkM = getMonoSink(CommittedCommand.key());

        if (sinkM.isPresent()) {
            log.warn("commit request already in progress!");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(CommittedCommand.key(), monoSink);
            hydraWSClient.commit();
        });
    }

    public Mono<CommittedResponse> commitFundsToTheHead(Map<String, UTXO> commitMap) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Initializing) {
            log.warn("Hydra needs to be in Initializing state!");

            return Mono.empty();
        }

        var sinkM = getMonoSink(CommittedCommand.key());

        if (sinkM.isPresent()) {
            log.warn("commit request already in progress!");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(CommittedCommand.key(), monoSink);
            hydraWSClient.commit(commitMap);
        });
    }

    public Mono<HeadIsFinalizedResponse> fanOutHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != FanoutPossible) {
            log.warn("Hydra needs to be in FanoutPossible state!");

            return Mono.empty();
        }

        var sinkM = getMonoSink(FanOutHeadCommand.key());

        if (sinkM.isPresent()) {
            log.warn("fanOutHead request already in progress!");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(FanOutHeadCommand.key(), monoSink);
            hydraWSClient.fanOut();
        });
    }

    public Mono<HeadIsClosedResponse> closeHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra needs to be in the Open state!");

            return Mono.empty();
        }

        var sinkM = getMonoSink(CloseHeadCommand.key());

        if (sinkM.isPresent()) {
            log.warn("closeHead request already in progress!");

            return Mono.empty();
        }

        return Mono.create(monoSink -> {
            storeMonoSinkReference(CloseHeadCommand.key(), monoSink);
            hydraWSClient.closeHead();
        });
    }

}