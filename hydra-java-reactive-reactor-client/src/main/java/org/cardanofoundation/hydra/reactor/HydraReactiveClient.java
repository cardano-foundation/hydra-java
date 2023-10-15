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

    /**
     * Default timeout duration for requests.
     */
    private static final Duration DEF_TIMEOUT_DURATION = Duration.ofMinutes(1);

    /**
     * WebSocket client for communication with the Hydra network.
     */
    @Nullable private HydraWSClient hydraWSClient;

    /**
     * Options for configuring the Hydra client.
     */
    private final HydraClientOptions hydraClientOptions;

    /**
     * Timeout duration for requests.
     */
    private final Duration timeout;

    /**
     * Map to store MonoSink references for ongoing requests.
     */
    private Map<String, MonoSink> monoSinkMap = new ConcurrentHashMap<>();

    /**
     * Constructs a HydraReactiveClient with the given parameters.
     *
     * @param uTxOStore The UTXO store for Hydra.
     * @param baseUrl   The base URL for Hydra communication.
     */
    public HydraReactiveClient(UTxOStore uTxOStore,
                               String baseUrl) {
        this(uTxOStore, baseUrl, DEF_TIMEOUT_DURATION);
    }

    /**
     * Constructs a HydraReactiveClient with the given parameters.
     *
     * @param uTxOStore The UTXO store for Hydra.
     * @param baseUrl   The base URL for Hydra communication.
     * @param timeout   The timeout duration for requests.
     */
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

    /**
     * Initializes the WebSocket client for communication with Hydra.
     */
    private void initWSClient() {
        if (this.hydraWSClient == null) {
            this.hydraWSClient = new HydraWSClient(hydraClientOptions);
            hydraWSClient.addHydraQueryEventListener(this);
            this.monoSinkMap = new ConcurrentHashMap<>();
        }
    }

    /**
     * Gets a reactive stream of Hydra states.
     *
     * @return A Flux emitting BiHydraState objects.
     */
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

    /**
     * Gets a reactive stream of Hydra responses.
     *
     * @return A Flux emitting Response objects.
     */
    public Flux<Response> getHydraResponsesStream() {
        if (hydraWSClient == null) {
            return Flux.empty();
        }

        // TODO when we close the connection how do we terminate the flux here?
        var adapter = new FluxSinkResponseAdapter();
        return Flux.<Response>create(fluxSink -> {
            adapter.setSink(fluxSink);
            hydraWSClient.addHydraQueryEventListener(adapter);
        }).doFinally(signal -> {
            log.debug("Removing hydra query event listener...");
            hydraWSClient.removeHydraQueryEventListener(adapter);
        });
    }

    /**
     * Destroys the WebSocket client and clears event listeners.
     */
    private void destroyWSClient() {
        if (hydraWSClient != null) {
            hydraWSClient.clearAllHydraQueryEventListeners();
            hydraWSClient.clearAllHydraStateEventListeners();
        }
        this.monoSinkMap = new ConcurrentHashMap<>();

        this.hydraWSClient = null;
    }

    /**
     * Handles Hydra responses and triggers corresponding actions.
     *
     * WARNING: internal API, subject to remove in the future
     *
     * @param response The Hydra response.
     */
    @Override
    public void onResponse(Response response) {
        log.debug("Tag:{}, seq:{}", response.getTag(), response.getSeq());
        log.info("monoSinkMap current size: {}", monoSinkMap.size()); // this is debugging only

        if (response instanceof GreetingsResponse gr) {
            var snapshotUtxo = gr.getSnapshotUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(snapshotUtxo);

            applyMonoSuccess(ConnectCommand.key(), gr);
        }

        if (response instanceof HeadIsOpenResponse ho) {
            hydraClientOptions.getUtxoStore().storeLatestUtxO(ho.getUtxo());
        }

        if (response instanceof CommittedResponse cr) {
            var utxoMap = cr.getUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxoMap);

            applyMonoSuccess(CommittedCommand.key(), cr);
        }

        if (response instanceof HeadIsClosedResponse hc) {
            applyMonoSuccess(CloseHeadCommand.key(), hc);
        }

        if (response instanceof SnapshotConfirmed sc) {
            var utxo = sc.getSnapshot().getUtxo();

            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);

            for (var txId : sc.getSnapshot().getConfirmedTransactions()) {
                var txSubmitGlobalCommand = TxSubmitGlobalCommand.of(txId);

                var txResult = new TxConfirmedResult(txId);

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

        if (response instanceof TxValidResponse txValidResponse) {
            var txId = txValidResponse.getTransaction().getId();
            var txResult = new TxResult(txId, true);

            applyMonoSuccess(TxSubmitLocalCommand.of(txId).key(), txResult);
        }
        if (response instanceof TxInvalidResponse txInvalidResponse) {
            var txId = txInvalidResponse.getTransaction().getId();
            var reason = txInvalidResponse.getValidationError().getReason();

            var txResult = new TxResult(txId, false, reason);

            applyMonoSuccess(TxSubmitLocalCommand.of(txId).key(), txResult);
        }

        if (response instanceof GetUTxOResponse getUTxOResponse) {
            applyMonoSuccess(GetUTxOCommand.key(), getUTxOResponse);
        }
    }

    /**
     * Gets the current state of the Hydra network.
     *
     * @return The current Hydra state.
     */
    public HydraState getHydraState() {
        if (hydraWSClient == null) {
            return Unknown;
        }

        return hydraWSClient.getHydraState();
    }

    /**
     * Gets the UTXOs from the Hydra network.
     *
     * @return A Mono emitting the GetUTxOResponse.
     */
    public Mono<GetUTxOResponse> getUTxOs() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra head is not open yet!");

            return Mono.empty();
        }

        var commandKey = GetUTxOCommand.key();
        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("getUTxO request already in progress...");

            return Mono.empty();
        }

        return Mono.<GetUTxOResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.getUTXO();
        })
        .timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("getUTxOs rquest timeout!"));
        }));
    }

    /**
     * Submit transaction to the hydra network and do not wait for confirmation of this transaction
     * from all head participants.
     *
     * @param txId
     * @param txCbor
     * @return
     */
    public Mono<TxResult> submitTx(String txId, byte[] txCbor) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra head is not open yet!");

            return Mono.empty();
        }

        var commandKey = TxSubmitLocalCommand.of(txId).key();

        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("tx submit request already in progress...");

            return Mono.empty();
        }

        return Mono.<TxResult>create(monoSink -> {
                    storeMonoSinkReference(commandKey, monoSink);
                    hydraWSClient.submitTx(encodeHexString(txCbor));
                })
                .timeout(timeout, Mono.defer(() -> {
                    applyMonoCleanup(commandKey);

                    return Mono.error(new TimeoutException("TxLocalSubmit timeout, txId: " + txId));
                }));
    }

    /**
     * Submit transaction to the hydra head network and wait for all head participants to confirm it.
     *
     * @param txId
     * @param txCbor
     * @return
     */
    public Mono<TxResult> submitTxFullConfirmation(String txId,
                                                   byte[] txCbor) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra head is not open yet!");

            return Mono.empty();
        }

        var commandKey = TxSubmitGlobalCommand.of(txId).key();

        var localTxMono = submitTx(txId, txCbor);

        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("tx submit request already in progress...");

            return Mono.empty();
        }

        return localTxMono
                .flatMap(localTxResult -> {
                    return Mono.<TxResult>create(monoSink -> {
                        storeMonoSinkReference(commandKey, monoSink);
                    })
                    .map(txConfirmedResult -> {
                        var confirmedTxId = txConfirmedResult.getTxId();
                        var isTxValid = localTxResult.isValid();
                        var reason = localTxResult.getReason();

                        return new TxResult(confirmedTxId, isTxValid, reason);
                    });
                })
                .timeout(timeout, Mono.defer(() -> {
                    applyMonoCleanup(commandKey);

                    return Mono.error(new TimeoutException("TxGlobalSubmit timeout, txId: " + txId));
                }));
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

    protected <T extends Request> void applyMonoError(String key,
                                                      Object result) {
        var monoSink = monoSinkMap.remove(key);

        monoSink.error(new HydraException(String.valueOf(result)));
    }

    protected  void applyMonoCleanup(String key) {
        monoSinkMap.remove(key);
    }

    /**
     * Opens a connection to the Hydra network.
     *
     * @return A Mono emitting GreetingsResponse upon successful connection.
     */
    public Mono<GreetingsResponse> openConnection() {
        initWSClient();

        assert hydraWSClient != null;

        if (hydraWSClient.isOpen()) {
            log.warn("Connection already open!");

            return Mono.empty();
        }

        var commandKey = ConnectCommand.key();
        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("connect request already in progress...");

            return Mono.empty();
        }

        return Mono.<GreetingsResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.connect();
        }).timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("head connect timeout!"));
        }));
    }

    /**
     * Closes the connection to the Hydra network.
     *
     * @return A Mono emitting true if the connection is closed successfully.
     * @throws InterruptedException If the thread is interrupted during the close operation.
     */
    public Mono<Boolean> closeConnection() throws InterruptedException {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        hydraWSClient.closeBlocking();
        destroyWSClient();

        return Mono.just(true);
    }

    /**
     * Aborts the head initialization process.
     *
     * @return A Mono emitting HeadIsAbortedResponse upon successful abortion.
     */
    public Mono<HeadIsAbortedResponse> abortHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Initializing) {
            log.warn("Hydra not in initializing state...");

            return Mono.empty();
        }

        var commandKey = AbortHeadCommand.key();
        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("abortHead request already in progress...");

            return Mono.empty();
        }

        return Mono.<HeadIsAbortedResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.abort();
        }).timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("abortHead timeout!"));
        }));
    }

    /**
     * Initiates the head initialization process.
     *
     * @return A Mono emitting HeadIsInitializingResponse upon successful initiation.
     */
    public Mono<HeadIsInitializingResponse> initHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Idle && hydraWSClient.getHydraState() != Final) {
            log.warn("Hydra needs to be either in Idle or Final state!");

            return Mono.empty();
        }

        var commandKey = InitHeadCommand.key();
        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("init head request already in progress!");

            return Mono.empty();
        }

        return Mono.<HeadIsInitializingResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.init();
        })
        .timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("initHead timeout"));
        }));
    }

    /**
     * Commits empty data to the Hydra head during initialization.
     *
     * @return A Mono emitting CommittedResponse upon successful commit.
     */
    public Mono<CommittedResponse> commitEmptyToTheHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Initializing) {
            log.warn("Hydra needs to be in initialising state!");

            return Mono.empty();
        }

        var commandKey = CommittedCommand.key();

        var sinkM = getMonoSink(commandKey);
        if (sinkM.isPresent()) {
            log.warn("commit request already in progress!");

            return Mono.empty();
        }

        return Mono.<CommittedResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.commit();
        })
        .timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("commit funds timeout!"));
        }));
    }

    /**
     * Commits funds to the Hydra head during initialization.
     *
     * @param commitMap The map of commit data.
     * @return A Mono emitting CommittedResponse upon successful commit.
     */
    public Mono<CommittedResponse> commitFundsToTheHead(Map<String, UTXO> commitMap) {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Initializing) {
            log.warn("Hydra needs to be in Initializing state!");

            return Mono.empty();
        }

        var commandKey = CommittedCommand.key();
        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("commit request already in progress!");

            return Mono.empty();
        }

        return Mono.<CommittedResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.commit(commitMap);
        })
        .timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("commit funds timeout!"));
        }));
    }

    /**
     * Initializes the head fanout, committing snapshot state back to Cardano's L1.
     *
     * @return A Mono emitting HeadIsFinalizedResponse upon successful fanout.
     */
    public Mono<HeadIsFinalizedResponse> fanOutHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != FanoutPossible) {
            log.warn("Hydra needs to be in FanoutPossible state!");

            return Mono.empty();
        }

        var commandKey = FanOutHeadCommand.key();

        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("fanOutHead request already in progress!");

            return Mono.empty();
        }

        return Mono.<HeadIsFinalizedResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.fanOut();
        }).timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("commit funds timeout!"));
        }));
    }

    /**
     * Initiates the request to clos Hydra's head.
     *
     * @return A Mono emitting HeadIsClosedResponse upon successful initiation of head closing.
     */
    public Mono<HeadIsClosedResponse> closeHead() {
        if (hydraWSClient == null) {
            return Mono.empty();
        }

        if (hydraWSClient.getHydraState() != Open) {
            log.warn("Hydra needs to be in the Open state!");

            return Mono.empty();
        }

        var commandKey = CloseHeadCommand.key();
        var sinkM = getMonoSink(commandKey);

        if (sinkM.isPresent()) {
            log.warn("closeHead request already in progress!");

            return Mono.empty();
        }

        return Mono.<HeadIsClosedResponse>create(monoSink -> {
            storeMonoSinkReference(commandKey, monoSink);
            hydraWSClient.closeHead();
        }).timeout(timeout, Mono.defer(() -> {
            applyMonoCleanup(commandKey);

            return Mono.error(new TimeoutException("closeHead timeout!"));
        }));
    }

}
