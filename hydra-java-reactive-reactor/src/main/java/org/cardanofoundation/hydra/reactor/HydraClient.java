package org.cardanofoundation.hydra.reactor;

import com.bloxbean.cardano.client.transaction.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.Request;
import org.cardanofoundation.hydra.core.model.Transaction;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.*;
import org.cardanofoundation.hydra.reactor.commands.ConnectCommand;
import org.cardanofoundation.hydra.reactor.commands.TxGlobalCommand;
import org.cardanofoundation.hydra.reactor.commands.TxLocalCommand;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.bloxbean.cardano.client.util.HexUtil.encodeHexString;
import static org.cardanofoundation.hydra.core.model.HydraState.*;

@Slf4j
public class HydraClient extends HydraQueryEventListener.Stub {

    @Nullable private HydraWSClient hydraWSClient;

    private final HydraClientOptions hydraClientOptions;

    public HydraClient(HydraClientOptions hydraClientOptions) {
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

    private void destroyWSClient() {
        hydraWSClient.removeHydraQueryEventListener(this);
        this.monoSinkMap = new ConcurrentHashMap<>();

        this.hydraWSClient = null;
    }

    @Override
    public void onResponse(Response response) {
        log.debug("Tag:{}, seq:{}", response.getTag(), response.getSeq());

        if (response instanceof HeadIsOpenResponse) {
            HeadIsOpenResponse ho = (HeadIsOpenResponse) response;
            // we get initial UTxOs here as well
            var utxo = ho.getUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);
        }

        if (response instanceof SnapshotConfirmed) {
            SnapshotConfirmed sc = (SnapshotConfirmed) response;
            Map<String, UTXO> utxo = sc.getSnapshot().getUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);

            for (Transaction trx : sc.getSnapshot().getConfirmedTransactions()) {
                TxResult txResult = new TxResult(trx.getId(), trx.getIsValid());

                TxGlobalCommand txGlobalCommand = TxGlobalCommand.of(trx.getId());
                applyMonoSuccess(txGlobalCommand.key(), txResult);
            }
        }

        if (response instanceof GreetingsResponse) {
            GreetingsResponse gr = (GreetingsResponse) response;
            var utxo = gr.getSnapshotUtxo();
            hydraClientOptions.getUtxoStore().storeLatestUtxO(utxo);

            applyMonoSuccess(ConnectCommand.key());
        }

        if (response instanceof TxValidResponse) {
            TxValidResponse txResponse = (TxValidResponse) response;
            String txId = txResponse.getTransaction().getId();
            TxResult txResult = new TxResult(txId, true);

            applyMonoSuccess(TxLocalCommand.of(txId).toString(), txResult);
        }
        if (response instanceof TxInvalidResponse) {
            TxInvalidResponse txResponse = (TxInvalidResponse) response;
            String txId = txResponse.getTransaction().getId();
            String reason = txResponse.getValidationError().getReason();
            TxResult txResult = new TxResult(txId, true, reason);

            applyMonoSuccess(TxLocalCommand.of(txId).key(), txResult);
        }
    }

    public HydraState getHydraState() {
        if (hydraWSClient == null) {
            return Unknown;
        }

        return hydraWSClient.getHydraState();
    }

    public Mono<TxResult> submitTx(byte[] cborTx) {
        return Mono.create(monoSink -> {
            String txHash = TransactionUtil.getTxHash(cborTx);
            storeMonoSinkReference(TxLocalCommand.of(txHash).key(), monoSink);
            hydraWSClient.submitTx(encodeHexString(cborTx));
        });
    }

    public Mono<TxResult> submitTxFullConfirmation(byte[] cborTx) {
        return Mono.create(monoSink -> {
            String txHash = TransactionUtil.getTxHash(cborTx);
            log.debug("Submitting tx:" + txHash);

            storeMonoSinkReference(TxGlobalCommand.of(txHash).key(), monoSink);
            hydraWSClient.submitTx(encodeHexString(cborTx));
        });
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

    public Mono<Void> openConnection() {
        initWSClient();

        if (!hydraWSClient.isOpen()) {
            return Mono.create(monoSink -> {
                storeMonoSinkReference(ConnectCommand.key(), monoSink);
                hydraWSClient.connect();
            });
        }

        return Mono.empty();
    }

    public Mono<Void> closeConnection() throws InterruptedException {
        if (hydraWSClient.isOpen()) {
            hydraWSClient.closeBlocking();
            destroyWSClient();

            return Mono.empty();
        }

        return Mono.empty();
    }

    public boolean abortHead() {
        if (hydraWSClient.getHydraState() == Initializing) {
            hydraWSClient.abort();

            return true;
        }

        return false;
    }

    public boolean initHead() {
        if (hydraWSClient.getHydraState() == Idle) {
            hydraWSClient.init();

            return true;
        }

        return false;
    }

    public boolean commitEmptyToTheHead() {
        if (hydraWSClient.getHydraState() == Initializing) {
            hydraWSClient.commit();

            return true;
        }

        return false;
    }

    public boolean commitFundsToTheHead(Map<String, UTXO> commitMap) {
        if (hydraWSClient.getHydraState() == Initializing) {
            hydraWSClient.commit(commitMap);

            return true;
        }

        return false;
    }

    public boolean fanOutHead() {
        if (hydraWSClient.getHydraState() == FanoutPossible) {
            hydraWSClient.fanOut();

            return true;
        }

        return false;
    }

    public boolean closeHead() {
        if (hydraWSClient.getHydraState() == Open) {
            hydraWSClient.closeHead();

            return true;
        }

        return false;
    }

}