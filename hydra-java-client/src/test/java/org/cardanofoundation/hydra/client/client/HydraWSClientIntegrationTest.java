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
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.*;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.client.client.helpers.HydraDevNetwork.HYDRA_ALICE_REMOTE_PORT;
import static org.cardanofoundation.hydra.core.model.HydraState.*;
import static org.cardanofoundation.hydra.core.utils.HexUtils.encodeHexString;

@Slf4j
public class HydraWSClientIntegrationTest {

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
        log.info("Alice:{}", ALICE_OPERATOR);
        log.info("Bob:{}", BOB_OPERATOR);
    }

    /**
     * Integration tests which tests library and the whole journey from:
     * connection, HydraState.Init -> HydraState.Open -> HydraState.Closed -> HydraState.FanoutPossible -> HydraState.Final
     * and finally closing the connection. In top of that, this test will test if Greetings message contains hydra state and initial utxos (empty).
     */
    @Test
    public void testHydraNetworkReachesOpenState() throws InterruptedException {
        var stopWatch = Stopwatch.createStarted();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var aliceHydraContainer = hydraDevNetwork.getAliceHydraContainer();
            var bobHydraContainer = hydraDevNetwork.getBobHydraContainer();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(aliceHydraContainer, HYDRA_ALICE_REMOTE_PORT))
                    .withUTxOStore(new InMemoryUTxOStore())
                    .build());
            aliceHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "alice"));

            var errorFuture = new CompletableFuture<Response>().orTimeout(1, MINUTES);;

            var aliceGreetingsFuture = new CompletableFuture<GreetingsResponse>().orTimeout(1, MINUTES);;
            var bobGreetingsFuture = new CompletableFuture<GreetingsResponse>().orTimeout(1, MINUTES);;

            aliceHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {

                @Override
                public void onSuccess(Response response) {
                    if (response instanceof GreetingsResponse) {
                        aliceGreetingsFuture.complete((GreetingsResponse) response);
                    }
                }

                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
               }
            });

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(bobHydraContainer, HydraDevNetwork.HYDRA_BOB_REMOTE_PORT))
                    .withUTxOStore(new InMemoryUTxOStore())
                    .build());
            bobHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "bob"));
            bobHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {

                @Override
                public void onSuccess(Response response) {
                    if (response instanceof GreetingsResponse) {
                        bobGreetingsFuture.complete((GreetingsResponse) response);
                    }
                }

                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
                }
            });

            aliceHydraWSClient.connectBlocking(1, MINUTES);
            bobHydraWSClient.connectBlocking(1, MINUTES);

            log.info("Check if greetings message was set to alice and contains hydra state and contains empty utxo snapshot...");
            await().atMost(Duration.ofMinutes(1))
                    .until(() -> {
                        return aliceGreetingsFuture.isDone()
                                && aliceGreetingsFuture.get().getHeadStatus() == Idle
                                && aliceGreetingsFuture.get().getSnapshotUtxo().isEmpty();
                    });

            log.info("Check if greetings message was set to bob and contains hydra state and contains empty utxo snapshot...");
            await().atMost(Duration.ofMinutes(1))
                    .until(() -> {
                        return bobGreetingsFuture.isDone()
                                && bobGreetingsFuture.get().getHeadStatus() == Idle
                                && bobGreetingsFuture.get().getSnapshotUtxo().isEmpty();
                    });

            log.info("Alice is sending init to the head... (only one actor needs to do it)");
            aliceHydraWSClient.init();

            log.info("Awaiting for HydraState[Initializing] from alice hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Initializing));

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Initializing));

            var aliceUtxo = new UTXO();
            aliceUtxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000 * 1_000_000)));

            aliceHydraWSClient.commit("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0", aliceUtxo);
            bobHydraWSClient.commit();

            await()
                    .atMost(Duration.ofMinutes(1))
                     .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Open));
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Open));

            log.info("Bob decided to close the head...");
            bobHydraWSClient.closeHead();

            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Closed));
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Closed));

            log.info("Now we need to wait a few minutes for contestation deadline...");
            await().atMost(Duration.ofMinutes(10))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(FanoutPossible));

            await().atMost(Duration.ofMinutes(10))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(FanoutPossible));

            // it is enough that any participant fans out
            bobHydraWSClient.fanOut();

            await()
                    .failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(10)).untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Final));
            await()
                    .failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(10)).untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Final));

            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

    // init and then abort -> final
    @Test
    public void testHydraNetworkReachesAbortState() throws InterruptedException {
        var stopWatch = Stopwatch.createStarted();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var errorFuture = new CompletableFuture<Response>().orTimeout(1, MINUTES);;

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getAliceHydraContainer(), HYDRA_ALICE_REMOTE_PORT))
                    .withUTxOStore(new InMemoryUTxOStore())
                    .build());
            aliceHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "alice"));
            aliceHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
                }
            });

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getBobHydraContainer(), HydraDevNetwork.HYDRA_BOB_REMOTE_PORT))
                    .withUTxOStore(new InMemoryUTxOStore())
                    .build());
            bobHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "bob"));
            bobHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
                }
            });

            aliceHydraWSClient.connectBlocking(1, MINUTES);
            bobHydraWSClient.connectBlocking(1, MINUTES);

            aliceHydraWSClient.init();

            log.info("Awaiting for HydraState[Initializing] from alice hydra node..");
            await().failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState())
                    .isEqualTo(Initializing)
                    );

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState())
                    .isEqualTo(Initializing));

            log.info("Alice is aborting...");
            aliceHydraWSClient.abort();

            log.info("Awaiting for HydraState[Final] from alice hydra node..");
            await().failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState())
                    .isEqualTo(Final));

            log.info("Awaiting for HydraState[Final] from bob hydra node..");
            await().failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState())
                    .isEqualTo(Final));

            log.info("Closing alice and bob connections...");
            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

    // tests head opening, getting utxo map (initial utxo snapshot) and send one simple transaction from alice to bob

    /**
     * Scenario tests:
     * - asserts head opening
     * - asserts initial utxo snapshot
     * - send simple ada transaction
     * - assert that re-connection with full history works
     */
    @Test
    public void testHydraOpeningWithInitialSnapshotAndSendingTransaction() throws InterruptedException, CborSerializationException {
        var stopWatch = Stopwatch.createStarted();
        var aliceInMemoryStore = new InMemoryUTxOStore();
        var bobInMemoryStore = new InMemoryUTxOStore();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getAliceHydraContainer(), HYDRA_ALICE_REMOTE_PORT))
                    .withUTxOStore(aliceInMemoryStore)
                    .build());
            aliceHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "alice"));

            var errorFuture = new CompletableFuture<Response>().orTimeout(1, MINUTES);;

            var aliceSnapshotReceived = new CompletableFuture<SnapshotConfirmed>().orTimeout(1, MINUTES);;
            var bobSnapshotConfirmed = new CompletableFuture<SnapshotConfirmed>().orTimeout(1, MINUTES);;

            var aliceHeadIsOpen = new CompletableFuture<HeadIsOpenResponse>().orTimeout(1, MINUTES);;
            var bobHeadIsOpen = new CompletableFuture<HeadIsOpenResponse>().orTimeout(1, MINUTES);;

            var aliceTxValidResponse = new CompletableFuture<TxValidResponse>().orTimeout(1, MINUTES);;
            var aliceTxInvalidResponse = new CompletableFuture<TxInvalidResponse>().orTimeout(1, MINUTES);;

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
                    if (response instanceof TxInvalidResponse) {
                        aliceTxInvalidResponse.complete((TxInvalidResponse) response);
                    }
                }

                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
                }
            });

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getBobHydraContainer(), HydraDevNetwork.HYDRA_BOB_REMOTE_PORT))
                    .withUTxOStore(bobInMemoryStore)
                    .build());
            bobHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "bob"));
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
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Initializing));

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Initializing));

            var aliceUtxo = new UTXO();
            aliceUtxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000 * 1_000_000)));

            aliceHydraWSClient.commit("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0", aliceUtxo);
            bobHydraWSClient.commit();

            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Open));
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Open));

            log.info("Check if alice receives head is open...");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> {
                        if (!aliceHeadIsOpen.isDone()) {
                            return false;
                        }
                        var headIsOpenResponse = aliceHeadIsOpen.get();

                        var utxo = headIsOpenResponse.getUtxo().get("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0");

                        return utxo.getAddress().equals("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3")
                                && utxo.getValue().get("lovelace").longValue() == 1000000000L;
                    });

            log.info("Check if bob receives head is open...");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> {
                        if (!bobHeadIsOpen.isDone()) {
                            return false;
                        }
                        var headIsOpenResponse = bobHeadIsOpen.get();

                        var utxo = headIsOpenResponse.getUtxo().get("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0");

                        return utxo.getAddress().equals("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3")
                                && utxo.getValue().get("lovelace").longValue() == 1000000000L;
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
                        System.out.println(snapshotConfirmed);

                        return !snapshotConfirmed.getSnapshot().getUtxo().isEmpty();
                    });

            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();

            log.info("Let's connect now with full history...");

            aliceInMemoryStore = new InMemoryUTxOStore();
            var aliceHydraWSClient2 = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getAliceHydraContainer(), HYDRA_ALICE_REMOTE_PORT))
                    .withUTxOStore(aliceInMemoryStore)
                    .history(true) // now lets see if connecting with history is fine
                    .build());

            var aliceGreetings = new CompletableFuture<GreetingsResponse>().orTimeout(1, MINUTES);
            aliceHydraWSClient2.addHydraQueryEventListener(new SLF4JHydraLogger(log, "alice"));
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
                        var hasUTxOs = !greetingsResponse.getSnapshotUtxo().isEmpty();

                        System.out.println(greetingsResponse.getSnapshotUtxo());

                        return isOpen && hasUTxOs;
                    });
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

    // More scenarios to cover:
    // - open a head, create a few transactions in the network after which close the head and fanout
    // - open a head, create smart contract transaction, e.g. to mint a token and redeem it (gift card contract)
    // - open a head, create a few transactions, reconnect with history = 0
    // - open a head, create a few transactions, reconnect with history = 1 and check replaying of all past events
    // - open a head and generate a few invalid transactions / commands and check if they are handled properly
    // - open a head mint NFT and try to close and move snapshot to L1
    // - open a head, create a few transactions using Snapshot received, disconnect, reconnect to see if utxos are present
    // - open a head and check that Greetings message sends you utxo snapshot and the current hydra state
    // - open a head and using getUtxO request and response validate that we handle those properly

}
