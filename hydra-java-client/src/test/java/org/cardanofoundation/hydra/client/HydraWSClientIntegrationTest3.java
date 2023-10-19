package org.cardanofoundation.hydra.client;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.cardano.client.lib.submit.HttpCardanoTxSubmissionService;
import org.cardanofoundation.hydra.cardano.client.lib.params.HydraNodeProtocolParametersAdapter;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.JacksonClasspathSecretKeyCardanoOperatorSupplier;
import org.cardanofoundation.hydra.cardano.client.lib.utxo.SnapshotUTxOSupplier;
import org.cardanofoundation.hydra.client.helpers.HydraTransactionGenerator;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.*;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.cardanofoundation.hydra.test.HydraDevNetwork;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.cardano.client.lib.utils.TransactionSigningUtil.sign;
import static org.cardanofoundation.hydra.core.model.HydraState.Initializing;
import static org.cardanofoundation.hydra.core.model.HydraState.Open;
import static org.cardanofoundation.hydra.core.utils.HexUtils.decodeHexString;
import static org.cardanofoundation.hydra.core.utils.HexUtils.encodeHexString;
import static org.cardanofoundation.hydra.test.HydraDevNetwork.getTxSubmitWebUrl;

@Slf4j
public class HydraWSClientIntegrationTest3 {

    private final static Network NETWORK = Networks.testnet();

    /**
     * Scenario tests:
     * - asserts head opening
     * - both alice and bob commit their UTxOs to the head
     * - asserts initial utxo snapshot for both alice and bob
     * - send simple ada transaction (alice sends bob 10 ADA)
     * - we assert that alice and bob gets TxValid and SnapshotConfirmed response with new UTxOs
     * - assert that re-connection with full history works
     */
    @Test
    public void testHydraOpeningWithInitialSnapshotAndSendingTransaction() throws InterruptedException, CborSerializationException {
        var stopWatch = Stopwatch.createStarted();
        var aliceInMemoryStore = new InMemoryUTxOStore();
        var bobInMemoryStore = new InMemoryUTxOStore();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var httpClient = HttpClient.newBuilder().build();
            var txSubmitWebUrl = getTxSubmitWebUrl(hydraDevNetwork.getTxSubmitContainer());
            log.info("Tx submit web url: {}", txSubmitWebUrl);
            var txSubmissionClient = new HttpCardanoTxSubmissionService(httpClient, txSubmitWebUrl);

            var aliceHydraWebClient = new HydraWebClient(HttpClient.newHttpClient(), HydraDevNetwork.getHydraApiWebUrl(hydraDevNetwork.getAliceHydraContainer()));
            var bobHydraWebClient = new HydraWebClient(HttpClient.newHttpClient(), HydraDevNetwork.getHydraApiWebUrl(hydraDevNetwork.getBobHydraContainer()));

            var protocolParamsSupplier = new HydraNodeProtocolParametersAdapter(aliceHydraWebClient.fetchProtocolParameters());
            var snapshotUTxOSupplier = new SnapshotUTxOSupplier(aliceInMemoryStore);

            var aliceOperator = new JacksonClasspathSecretKeyCardanoOperatorSupplier(
                    "devnet/credentials/alice-funds.sk",
                    NETWORK).getOperator();

            var bobOperator = new JacksonClasspathSecretKeyCardanoOperatorSupplier(
                    "devnet/credentials/bob-funds.sk",
                    NETWORK)
                    .getOperator();

            var errorFuture = new CompletableFuture<Response>();
            var aliceState = new AtomicReference<HydraState>();
            var bobState = new AtomicReference<HydraState>();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiWebSocketUrl(hydraDevNetwork.getAliceHydraContainer()))
                    .utxoStore(aliceInMemoryStore)
                    .snapshotUtxo(true)
                    .build());

            SLF4JHydraLogger aliceHydraLogger = SLF4JHydraLogger.of(log, "alice");
            aliceHydraWSClient.addHydraQueryEventListener(aliceHydraLogger);
            aliceHydraWSClient.addHydraStateEventListener(aliceHydraLogger);

            aliceHydraWSClient.addHydraStateEventListener((prevState, newState) -> aliceState.set(newState));

            var aliceSnapshotReceived = new CompletableFuture<SnapshotConfirmed>().orTimeout(1, MINUTES);
            var bobSnapshotConfirmed = new CompletableFuture<SnapshotConfirmed>().orTimeout(1, MINUTES);

            var aliceHeadIsOpen = new CompletableFuture<HeadIsOpenResponse>().orTimeout(1, MINUTES);
            var bobHeadIsOpen = new CompletableFuture<HeadIsOpenResponse>().orTimeout(1, MINUTES);

            var aliceTxValidResponse = new CompletableFuture<TxValidResponse>().orTimeout(1, MINUTES);
            var aliceTxInvalidResponse = new CompletableFuture<TxInvalidResponse>().orTimeout(1, MINUTES);

            aliceHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {

                @Override
                public void onSuccess(Response response) {
                    if (response instanceof SnapshotConfirmed) {
                        var sc = (SnapshotConfirmed) response;
                        aliceSnapshotReceived.complete(sc);
                    }
                    if (response instanceof HeadIsOpenResponse) {
                        aliceHeadIsOpen.complete((HeadIsOpenResponse) response);
                    }
                    if (response instanceof TxValidResponse) {
                        aliceTxValidResponse.complete((TxValidResponse) response);
                    }
                }

                @Override
                public void onFailure(Response response) {
                    if (response instanceof TxInvalidResponse) {
                        aliceTxInvalidResponse.complete((TxInvalidResponse) response);
                    }
                    errorFuture.complete(response);
                }
            });

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions
                    .builder(HydraDevNetwork.getHydraApiWebSocketUrl(hydraDevNetwork.getBobHydraContainer()))
                    .utxoStore(bobInMemoryStore)
                    .snapshotUtxo(true)
                    .build());
            SLF4JHydraLogger bobHydraLogger = SLF4JHydraLogger.of(log, "bob");
            bobHydraWSClient.addHydraQueryEventListener(bobHydraLogger);
            bobHydraWSClient.addHydraStateEventListener(bobHydraLogger);

            bobHydraWSClient.addHydraStateEventListener((prevState, newState) -> bobState.set(newState));

            bobHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {

                @Override
                public void onSuccess(Response response) {
                    if (response instanceof SnapshotConfirmed) {
                        var sc = (SnapshotConfirmed) response;
                        bobSnapshotConfirmed.complete(sc);
                    }
                    if (response instanceof HeadIsOpenResponse) {
                        bobHeadIsOpen.complete((HeadIsOpenResponse) response);
                    }

                }

                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
                }
            });

            aliceHydraWSClient.connectBlocking(1, MINUTES);
            bobHydraWSClient.connectBlocking(1, MINUTES);

            log.info("Alice is sending init to the head... (only one actor needs to do it)");
            aliceHydraWSClient.init();

            log.info("Awaiting for HydraState[Initializing] from alice hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> aliceState.get() == Initializing);

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> bobState.get() == Initializing);

            var aliceUtxo = new UTXO();
            aliceUtxo.setAddress("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(100 * 1_000_000)));

            var bobUtxo = new UTXO();
            bobUtxo.setAddress("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh");
            bobUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(100 * 1_000_000)));

            var aliceUtxoMap = Map.of("c8870bcd3602a685ba24b567ae5fec7ecab8500798b427d3d2f04605db1ea9fb#0", aliceUtxo);
            var bobUtxoMap = Map.of("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0", bobUtxo);

            var aliceHeadCommitted = aliceHydraWebClient.commitRequest(aliceUtxoMap);
            var bobHeadCommitted = bobHydraWebClient.commitRequest(bobUtxoMap);

            log.info("Alice head committed: {}", aliceHeadCommitted);
            log.info("Bob head committed: {}", bobHeadCommitted);

            var aliceCommitTxToSign = aliceHeadCommitted.getCborHex();
            var bobCommitTxToSign = bobHeadCommitted.getCborHex();

            var aliceCommitTxSigned = sign(decodeHexString(aliceCommitTxToSign), aliceOperator.getSecretKey());
            var bobCommitTxSigned = sign(decodeHexString(bobCommitTxToSign), bobOperator.getSecretKey());

            log.info("Alice aliceCommitTxSigned: {}", aliceCommitTxSigned);
            log.info("Bob bobCommitTxSigned: {}", bobCommitTxSigned);

            var aliceCommitResult = txSubmissionClient.submitTransaction(aliceCommitTxSigned);
            var bobCommitResult = txSubmissionClient.submitTransaction(bobCommitTxSigned);

            if (!aliceCommitResult.isSuccessful()) {
                log.warn("Alice funds commitment failed, reason: {}", aliceCommitResult.getResponse());
            }

            if (!bobCommitResult.isSuccessful()) {
                log.warn("Bob funds commitment failed, reason: {}", bobCommitResult.getResponse());
            }

            Assertions.assertTrue(aliceCommitResult.isSuccessful());
            Assertions.assertTrue(bobCommitResult.isSuccessful());

            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> aliceState.get() == Open);
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> bobState.get() == Open);

            log.info("Check if alice receives head is open...");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> {
                        if (!aliceHeadIsOpen.isDone()) {
                            return false;
                        }
                        var headIsOpenResponse = aliceHeadIsOpen.get();

                        var localAliceUtxo = headIsOpenResponse.getUtxo().get("c8870bcd3602a685ba24b567ae5fec7ecab8500798b427d3d2f04605db1ea9fb#0");
                        var localBobUtxo = headIsOpenResponse.getUtxo().get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0");

                        return localAliceUtxo.getAddress().equals("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z")
                                && localAliceUtxo.getValue().get("lovelace").longValue() == 100_000_000L
                                && localBobUtxo.getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh")
                                && localBobUtxo.getValue().get("lovelace").longValue() == 100_000_000L;
                    });

            log.info("Check if bob receives head is open...");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> {
                        if (!bobHeadIsOpen.isDone()) {
                            return false;
                        }
                        var headIsOpenResponse = bobHeadIsOpen.get();

                        var localAliceUtxo = headIsOpenResponse.getUtxo().get("c8870bcd3602a685ba24b567ae5fec7ecab8500798b427d3d2f04605db1ea9fb#0");
                        var localBobUtxo = headIsOpenResponse.getUtxo().get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0");

                        return localAliceUtxo.getAddress().equals("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z")
                                && localAliceUtxo.getValue().get("lovelace").longValue() == 100_000_000L
                                && localBobUtxo.getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh")
                                && localBobUtxo.getValue().get("lovelace").longValue() == 100_000_000L;
                    });

            var transactionSender = new HydraTransactionGenerator(snapshotUTxOSupplier, protocolParamsSupplier);

            log.info("Let's check if alice sends bob 10 ADA...");
            var trxBytes = transactionSender.simpleTransfer(aliceOperator, bobOperator, 10);

            aliceHydraWSClient.submitTx(encodeHexString(trxBytes));

            log.info("Let's check if alice transaction to send 10 to bob is successful...");

            await().atMost(Duration.ofMinutes(1))
                    .failFast(() -> errorFuture.isDone() || aliceTxInvalidResponse.isDone())
                    .until(() -> {
                        if (!aliceTxValidResponse.isDone()) {
                            return false;
                        }

                        var txValidResponse = aliceTxValidResponse.get();

                        return !txValidResponse.isFailure();
                    });

            log.info("Let's check if alice received SnapshotConfirmed message... (full consensus validation)");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> {
                        if (!aliceSnapshotReceived.isDone()) {
                            return false;
                        }

                        var snapshotConfirmed = aliceSnapshotReceived.get();

                        var snapshot = snapshotConfirmed.getSnapshot().getUtxo();

                        return
                            snapshot.get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0").getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh") &&
                            snapshot.get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0").getValue().get("lovelace").longValue() == 100_000_000L &&

                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#1").getAddress().equals("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z") &&
                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#1").getValue().get("lovelace").longValue() == 89834587L &&

                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#0").getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh") &&
                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#0").getValue().get("lovelace").longValue() == 10000000L;
                    });

            log.info("Let's check if bob received SnapshotConfirmed message... (full consensus validation)");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> {
                        if (!bobSnapshotConfirmed.isDone()) {
                            return false;
                        }

                        var snapshotConfirmed = bobSnapshotConfirmed.get();

                        var snapshot = snapshotConfirmed.getSnapshot().getUtxo();

                        return
                                snapshot.get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0").getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh") &&
                                snapshot.get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0").getValue().get("lovelace").longValue() == 100_000_000L &&

                                snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#1").getAddress().equals("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z") &&
                                snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#1").getValue().get("lovelace").longValue() == 89834587L &&

                                snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#0").getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh") &&
                                snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#0").getValue().get("lovelace").longValue() == 10000000L;
                    });

            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();

            log.info("Let's connect now with full history...");
            aliceInMemoryStore = new InMemoryUTxOStore();
            var aliceHydraWSClient2 = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiWebSocketUrl(hydraDevNetwork.getAliceHydraContainer()))
                    .utxoStore(aliceInMemoryStore)
                    .history(true) // now lets see if connecting with history is fine
                    .build());

            var aliceGreetings = new CompletableFuture<GreetingsResponse>().orTimeout(1, MINUTES);
            aliceHydraWSClient2.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "alice"));
            aliceHydraWSClient2.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
                @Override
                public void onSuccess(Response response) {
                    if (response instanceof GreetingsResponse) {
                        aliceGreetings.complete((GreetingsResponse) response);
                    }
                }
            });

            aliceHydraWSClient2.connectBlocking(1, MINUTES);

            await().atMost(Duration.ofMinutes(1))
                    .until(() -> {
                        if (!aliceGreetings.isDone()) {
                            return false;
                        }

                        var greetingsResponse = aliceGreetings.get();
                        var isOpen = greetingsResponse.getHeadStatus() == Open;
                        var snapshot = greetingsResponse.getSnapshotUtxo();

                        return isOpen &&
                            snapshot.get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0").getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh") &&
                            snapshot.get("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0").getValue().get("lovelace").longValue() == 100_000_000L &&

                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#1").getAddress().equals("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z") &&
                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#1").getValue().get("lovelace").longValue() == 89834587L &&

                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#0").getAddress().equals("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh") &&
                            snapshot.get("c12fdf25a74f5c58c50da90b4c4df6d2dc4e64e4c804858677001856ff4ac89d#0").getValue().get("lovelace").longValue() == 10000000L;
                    });
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

}
