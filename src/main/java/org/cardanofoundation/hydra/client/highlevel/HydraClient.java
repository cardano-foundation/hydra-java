package org.cardanofoundation.hydra.client.highlevel;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.Transaction;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.response.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.cardanofoundation.hydra.client.highlevel.ConfirmationType.Full;
import static org.cardanofoundation.hydra.client.highlevel.ConfirmationType.Partial;
import static org.cardanofoundation.hydra.client.utils.HexUtils.encodeHexString;

@Slf4j
public class HydraClient extends HydraQueryEventListener.Stub {

    private final HydraClientOptions hydraClientOptions;

    private HydraWSClient hydraWSClient;

    // TODO we need to expire and remove items from the map or it will grow and reach OOM
    // error handling
    private final Map<String, CompletableFuture<Object>> futuresMap = new ConcurrentHashMap<>();

    private final UTxOStore uTxOStore;

    public HydraClient(HydraClientOptions hydraClientOptions,
                       UTxOStore uTxOStore) {
        this.hydraClientOptions = hydraClientOptions;
        this.uTxOStore = uTxOStore;
    }

    // TODO error handling
    // what if you try to connect twice -> check that there is pending connection?
    public CompletableFuture<Connected> connect() {
        if (hydraWSClient != null && hydraWSClient.getHydraState() != HydraState.Unknown) {
            return CompletableFuture.failedFuture(new IllegalStateException("You cannot connect twice!"));
        }
        var connectKey = Connect.INSTANCE.key();
,
        if (futuresMap.containsKey(connectKey)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Connection in progress!"));
        }

        this.hydraWSClient = new HydraWSClient(hydraClientOptions);
        hydraWSClient.addHydraQueryEventListener(this);

        var connectFuture = new CompletableFuture<>();

        storeFuture(connectKey, connectFuture);

        hydraWSClient.connect();

        return connectFuture.thenApply(o -> (Connected) o);
    }

    public CompletableFuture<Void> init() {
        if (hydraWSClient != null && hydraWSClient.isOpen()) {
            var initFuture = new CompletableFuture<>().orTimeout(1, MINUTES);

            storeFuture(Init.INSTANCE.key(), initFuture);

            hydraWSClient.init();

            return initFuture.thenRun(() -> {});
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> commit() {
        if (hydraWSClient != null && hydraWSClient.isOpen()) {
            var commitFuture = new CompletableFuture<>();

            storeFuture(Commit.INSTANCE.key(), commitFuture);

            hydraWSClient.commit();

            return commitFuture.thenRun(() -> {});
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> commit(Map<String, UTXO> utxoMap) {
        if (hydraWSClient != null && hydraWSClient.isOpen()) {
            var commitFuture = new CompletableFuture<>();

            storeFuture(Commit.INSTANCE.key(), commitFuture);

            hydraWSClient.commit(utxoMap);

            return commitFuture.thenRun(() -> {});
        }

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> commit(String utxoId, UTXO utxo) {
        return commit(Map.of(utxoId, utxo));
    }

    public CompletableFuture<TxResult> submitTx(String txHash, byte[] cborTx, ConfirmationType confirmationType) {
        var txFuture = new CompletableFuture<>();

        storeFuture(TxRequest.of(txHash, confirmationType).key(), txFuture);

        hydraWSClient.newTx(encodeHexString(cborTx));

        return txFuture.thenApply(o -> (TxResult) o);
    }

    public void disconnect() {
        futuresMap.remove(Connect.INSTANCE.key());
        if (hydraWSClient != null) {
            hydraWSClient.close();
        }
    }

    @Override
    public void onResponse(org.cardanofoundation.hydra.client.model.query.response.Response response) {
        log.info("Tag:{}, seq:{}", response.getTag(), response.getSeq());

        if (response instanceof GreetingsResponse) {
            var gr = (GreetingsResponse) response;

            uTxOStore.storeLatestUtxO(gr.getSnapshotUtxo());

            resolveFutureSuccessForKey(Connect.INSTANCE.key(), Connected.builder().greetings(gr).build());
        }

        if (response instanceof HeadIsInitializingResponse) {
            var hinit = (HeadIsInitializingResponse) response;
            resolveFutureSuccessForKey(Init.INSTANCE.key(), hinit);
        }

        if (response instanceof HeadIsOpenResponse) {
            var ho = (HeadIsOpenResponse) response;
            var utxo = ho.getUtxo();

            uTxOStore.storeLatestUtxO(utxo);

            resolveFutureSuccessForKey(Commit.INSTANCE.key(), ho);
        }

        if (response instanceof SnapshotConfirmed) {
            var sc = (SnapshotConfirmed) response;
            var utxo = sc.getSnapshot().getUtxo();

            uTxOStore.storeLatestUtxO(utxo);

            for (Transaction trx : sc.getSnapshot().getConfirmedTransactions()) {
                TxResult txResult = new TxResult(trx.getId(), trx.getIsValid());

                var key = TxRequest.of(trx.getId(), Full).key();
                resolveFutureSuccessForKey(key, txResult);
            }
        }

        if (response instanceof TxValidResponse) {
            var txvr = (TxValidResponse) response;
            String txId = txvr.getTransaction().getId();
            TxResult txResult = new TxResult(txId, true);

            var key = TxRequest.of(txId, Partial).key();

            resolveFutureSuccessForKey(key, txResult);
        }

        if (response instanceof TxInvalidResponse) {
            var txivr = (TxInvalidResponse) response;

            String txId = txivr.getTransaction().getId();
            String reason = txivr.getValidationError().getReason();
            TxResult txResult = new TxResult(txId, true, reason);

            var key = TxRequest.of(txId, Partial).key();

            resolveFutureSuccessForKey(key, txResult);
        }
    }

    protected HydraState getHydraState() {
        return hydraWSClient.getHydraState();
    }

    protected void storeFuture(String key, CompletableFuture<Object> future) {
        futuresMap.computeIfAbsent(key, k -> {
            List<CompletableFuture<Object>> list = new ArrayList<>();
            list.add(future);

            return list;
        });
    }

    protected void resolveFutureSuccessForKey(String key, Object value) {
        List<CompletableFuture<Object>> futuresList = futuresMap.remove(key);
        if (futuresList == null) {
            return;
        }

        futuresList.forEach(f -> f.complete(value));
    }

    protected void resolveFutureFailureForKey(String key, Exception ex) {
        List<CompletableFuture<Object>> futuresList = futuresMap.remove(key);
        if (futuresList == null) {
            return;
        }

        futuresList.forEach(f -> f.completeExceptionally(ex));
    }

}
