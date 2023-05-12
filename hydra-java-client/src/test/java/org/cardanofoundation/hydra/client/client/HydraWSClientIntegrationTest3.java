package org.cardanofoundation.hydra.client.client;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.cardano.client.lib.HydraOperator;
import org.cardanofoundation.hydra.cardano.client.lib.JacksonClasspathProtocolParametersSupplier;
import org.cardanofoundation.hydra.cardano.client.lib.JacksonClasspathSecretKeySupplierHydra;
import org.cardanofoundation.hydra.cardano.client.lib.SnapshotUTxOSupplier;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.client.SLF4JHydraLogger;
import org.cardanofoundation.hydra.client.client.helpers.HydraDevNetwork;
import org.cardanofoundation.hydra.client.client.helpers.HydraTransactionGenerator;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.*;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.core.model.HydraState.Initializing;
import static org.cardanofoundation.hydra.core.model.HydraState.Open;
import static org.cardanofoundation.hydra.core.utils.HexUtils.encodeHexString;

@Slf4j
public class HydraWSClientIntegrationTest3 {

    private static ProtocolParamsSupplier PROTOCOL_PARAMS_SUPPLIER;

    private static HydraOperator ALICE_OPERATOR;

    private static HydraOperator BOB_OPERATOR;

    @BeforeAll
    public static void setUpOnce() throws CborSerializationException {
        var objectMapper = new ObjectMapper();
        // TODO when hydra supports protocol params via REST we can make it better
        PROTOCOL_PARAMS_SUPPLIER = new JacksonClasspathProtocolParametersSupplier(objectMapper);
        ALICE_OPERATOR = new JacksonClasspathSecretKeySupplierHydra(objectMapper, "devnet/credentials/alice.sk").getOperator();
        BOB_OPERATOR = new JacksonClasspathSecretKeySupplierHydra(objectMapper, "devnet/credentials/bob.sk").getOperator();
        log.info("Alice OPERATOR:{}", ALICE_OPERATOR);
        log.info("Bob OPERATOR:{}", BOB_OPERATOR);
    }

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

            var errorFuture = new CompletableFuture<Response>();
            var aliceState = new AtomicReference<HydraState>();
            var bobState = new AtomicReference<HydraState>();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getAliceHydraContainer()))
                    .withUTxOStore(aliceInMemoryStore)
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

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getBobHydraContainer()))
                    .withUTxOStore(bobInMemoryStore)
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
            aliceUtxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000 * 1_000_000)));

            var bobUtxo = new UTXO();
            bobUtxo.setAddress("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k");
            bobUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(500 * 1_000_000)));

            aliceHydraWSClient.commit("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0", aliceUtxo);
            bobHydraWSClient.commit("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0", bobUtxo);

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

                        var localAliceUtxo = headIsOpenResponse.getUtxo().get("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0");
                        var localBobUtxo = headIsOpenResponse.getUtxo().get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0");

                        return localAliceUtxo.getAddress().equals("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3")
                                && localAliceUtxo.getValue().get("lovelace").longValue() == 1000_000_000L
                                && localBobUtxo.getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k")
                                && localBobUtxo.getValue().get("lovelace").longValue() == 500_000_000L;
                    });

            log.info("Check if bob receives head is open...");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> {
                        if (!bobHeadIsOpen.isDone()) {
                            return false;
                        }
                        var headIsOpenResponse = bobHeadIsOpen.get();

                        var localAliceUtxo = headIsOpenResponse.getUtxo().get("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0");
                        var localBobUtxo = headIsOpenResponse.getUtxo().get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0");

                        return localAliceUtxo.getAddress().equals("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3")
                                && localAliceUtxo.getValue().get("lovelace").longValue() == 1000000000L
                                && localBobUtxo.getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k")
                                && localBobUtxo.getValue().get("lovelace").longValue() == 500_000_000L;
                    });

            var transactionSender = new HydraTransactionGenerator(new SnapshotUTxOSupplier(aliceHydraWSClient.getUtxoStore()), PROTOCOL_PARAMS_SUPPLIER);

            log.info("Let's check if alice sends bob 10 ADA...");
            var trxBytes = transactionSender.simpleTransfer(ALICE_OPERATOR, BOB_OPERATOR, 10);

            aliceHydraWSClient.submitTx(encodeHexString(trxBytes));

            log.info("Let's check if alice transaction to send 10 to bob is successful...");

            await().atMost(Duration.ofMinutes(1))
                    .failFast(() -> errorFuture.isDone() || aliceTxInvalidResponse.isDone())
                    .until(() -> {
                        if (!aliceTxValidResponse.isDone()) {
                            return false;
                        }

                        var txValidResponse = aliceTxValidResponse.get();

                        return !txValidResponse.isFailure() && txValidResponse.getTransaction().getIsValid();
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
                            snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#0").getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k") &&
                            snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#0").getValue().get("lovelace").longValue() == 10_000_000L && // 10 ADA

                            snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#1").getAddress().equals("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3") &&
                            snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#1").getValue().get("lovelace").longValue() == 989_834_587L && // 989 ADA

                            snapshot.get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0").getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k") &&
                            snapshot.get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0").getValue().get("lovelace").longValue() == 500_000_000L; // 500 ADA
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
                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#0").getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k") &&
                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#0").getValue().get("lovelace").longValue() == 10_000_000L && // 10 ADA

                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#1").getAddress().equals("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3") &&
                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#1").getValue().get("lovelace").longValue() == 989_834_587L && // 989 ADA

                                snapshot.get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0").getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k") &&
                                snapshot.get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0").getValue().get("lovelace").longValue() == 500_000_000L; // 500 ADA
                    });

            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();

            log.info("Let's connect now with full history...");
            aliceInMemoryStore = new InMemoryUTxOStore();
            var aliceHydraWSClient2 = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getAliceHydraContainer()))
                    .withUTxOStore(aliceInMemoryStore)
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
                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#0").getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k") &&
                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#0").getValue().get("lovelace").longValue() == 10_000_000L && // 10 ADA

                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#1").getAddress().equals("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3") &&
                                snapshot.get("b9f48dd61b739c7deb55a55bc8fe8097165379efcfa918010fec75de6c6b8f64#1").getValue().get("lovelace").longValue() == 989_834_587L && // 989 ADA

                                snapshot.get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0").getAddress().equals("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k") &&
                                snapshot.get("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0").getValue().get("lovelace").longValue() == 500_000_000L; // 500 ADA
                    });
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

}
