package org.cardanofoundation.hydra.reactor;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.Request;
import org.cardanofoundation.hydra.core.model.Transaction;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.*;
import org.cardanofoundation.hydra.reactor.commands.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cardanofoundation.hydra.core.model.HydraState.*;
import static org.cardanofoundation.hydra.core.model.Tag.FanoutTx;
import static org.cardanofoundation.hydra.core.utils.HexUtils.encodeHexString;

@Slf4j
public class HydraReactiveClient extends HydraQueryEventListener.Stub {

    @Nullable private HydraWSClient hydraWSClient;

    private final HydraClientOptions hydraClientOptions;

    public HydraReactiveClient(HydraClientOptions hydraClientOptions) {
        this.hydraClientOptions = hydraClientOptions;
    }

    private Map<String, List<MonoSink>> monoSinkMap = new ConcurrentHashMap<>();

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
        return Flux.<BiHydraState>create(fluxSink -> {
            adapter.setSink(fluxSink);
            hydraWSClient.addHydraStateEventListener((adapter));
        }).doFinally(signal -> {
            log.info("Removing hydra state event listener.");
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
            log.info("Removing hydra query event listener.");
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

        if (response instanceof GreetingsResponse) {
            GreetingsResponse gr = (GreetingsResponse) response;
            var utxo = gr.getSnapshotUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);

            applyMonoSuccess(ConnectCommand.key(), gr);
        }

        if (response instanceof HeadIsOpenResponse) {
            HeadIsOpenResponse ho = (HeadIsOpenResponse) response;
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

        if (response instanceof HeadIsClosedResponse) {
            HeadIsClosedResponse hc = (HeadIsClosedResponse) response;

            applyMonoSuccess(CloseHeadCommand.key(), hc);
        }

        if (response instanceof SnapshotConfirmed) {
            SnapshotConfirmed sc = (SnapshotConfirmed) response;
            Map<String, UTXO> utxo = sc.getSnapshot().getUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);

            for (Transaction trx : sc.getSnapshot().getConfirmedTransactions()) {
                TxResult txResult = new TxResult(trx.getId(), trx.getIsValid());

                TxSubmitGlobalCommand txSubmitGlobalCommand = TxSubmitGlobalCommand.of(trx.getId());
                applyMonoSuccess(txSubmitGlobalCommand.key(), txResult);
            }
        }

        if (response instanceof HeadIsInitializingResponse) {
            HeadIsInitializingResponse hi = (HeadIsInitializingResponse) response;

            applyMonoSuccess(InitHeadCommand.key(), hi);
        }

        if (response instanceof HeadIsAbortedResponse) {
            HeadIsAbortedResponse ha = (HeadIsAbortedResponse) response;

            applyMonoSuccess(AbortHeadCommand.key(), ha);
        }

        if (response instanceof HeadIsFinalizedResponse) {
            HeadIsFinalizedResponse hf = (HeadIsFinalizedResponse) response;

            applyMonoSuccess(FanOutHeadCommand.key(), hf);
        }

        if (response instanceof PostTxOnChainFailedResponse) {
            PostTxOnChainFailedResponse failure = (PostTxOnChainFailedResponse) response;

            if (failure.getPostChainTx().getTag() == FanoutTx) {
                applyMonoError(FanOutHeadCommand.key(), "Fanout failed.");
            }
        }

        if (response instanceof TxValidResponse) {
            TxValidResponse txResponse = (TxValidResponse) response;
            String txId = txResponse.getTransaction().getId();
            TxResult txResult = new TxResult(txId, true);

            applyMonoSuccess(TxSubmitLocalCommand.of(txId).toString(), txResult);
        }
        if (response instanceof TxInvalidResponse) {
            TxInvalidResponse txResponse = (TxInvalidResponse) response;
            String txId = txResponse.getTransaction().getId();
            String reason = txResponse.getValidationError().getReason();

            TxResult txResult = new TxResult(txId, false, reason);

            applyMonoSuccess(TxSubmitLocalCommand.of(txId).key(), txResult);
            applyMonoError(TxSubmitGlobalCommand.of(txId).key(), txResult);
        }
        if (response instanceof GetUTxOResponse) {
            GetUTxOResponse getUTxOResponse = (GetUTxOResponse) response;

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

        if (hydraWSClient.getHydraState() == Open) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(GetUTxOCommand.key(), monoSink);
                hydraWSClient.getUTXO();
            });
        }

        return Mono.empty();
    }

    public Mono<TxResult> submitTx(String txId, byte[] txCbor) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == Open) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(TxSubmitLocalCommand.of(txId).key(), monoSink);
                hydraWSClient.submitTx(encodeHexString(txCbor));
            });
        }

        return Mono.empty();
    }

    public Mono<TxResult> submitTxFullConfirmation(String txId, byte[] txCbor) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == Open) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(TxSubmitGlobalCommand.of(txId).key(), monoSink);
                hydraWSClient.submitTx(encodeHexString(txCbor));
            });
        }

        return Mono.empty();
    }

    protected void storeMonoSinkReference(String key, MonoSink monoSink) {
        monoSinkMap.computeIfAbsent(key, k -> {
            var list = new ArrayList<MonoSink>();
            list.add(monoSink);

            return list;
        });
    }

    protected <T extends Request> void applyMonoSuccess(String key, Object result) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null) {
            return;
        }

        monoSinks.forEach(monoSink -> monoSink.success(result));
    }

    protected <T extends Request> void applyMonoSuccess(String key) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null) {
            return;
        }

        monoSinks.forEach(MonoSink::success);
    }

    protected <T extends Request> void applyMonoError(String key, Object result) {
        List<MonoSink> monoSinks = monoSinkMap.remove(key);
        if (monoSinks == null) {
            return;
        }

        monoSinks.forEach(monoSink -> monoSink.error(new RuntimeException(String.valueOf(result))));
    }

    public Mono<GreetingsResponse> openConnection() {
        initWSClient();

        assert hydraWSClient != null;

        if (!hydraWSClient.isOpen()) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(ConnectCommand.key(), monoSink);
                hydraWSClient.connect();
            });
        }

        return Mono.empty();
    }

    public Mono<Boolean> closeConnection() throws InterruptedException {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.isOpen()) {
            hydraWSClient.closeBlocking();
            destroyWSClient();

            return Mono.just(true);
        }

        return Mono.empty();
    }

    public Mono<HeadIsAbortedResponse> abortHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == Initializing) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(AbortHeadCommand.key(), monoSink);
                hydraWSClient.abort();
            });
        }

        return Mono.empty();
    }

    public Mono<HeadIsInitializingResponse> initHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == Idle || hydraWSClient.getHydraState() == Final) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(InitHeadCommand.key(), monoSink);
                hydraWSClient.init();
            });
        }

        return Mono.empty();
    }

    public Mono<CommittedResponse> commitEmptyToTheHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == Initializing) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(CommittedCommand.key(), monoSink);
                hydraWSClient.commit();
            });
        }

        return Mono.empty();
    }

    public Mono<CommittedResponse> commitFundsToTheHead(Map<String, UTXO> commitMap) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == Initializing) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(CommittedCommand.key(), monoSink);
                hydraWSClient.commit(commitMap);
            });
        }

        return Mono.empty();
    }

    public Mono<HeadIsFinalizedResponse> fanOutHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == FanoutPossible) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(FanOutHeadCommand.key(), monoSink);
                hydraWSClient.fanOut();
            });
        }

        return Mono.empty();
    }

    public Mono<HeadIsClosedResponse> closeHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() == Open) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(CloseHeadCommand.key(), monoSink);
                hydraWSClient.closeHead();
            });
        }

        return Mono.empty();
    }

}