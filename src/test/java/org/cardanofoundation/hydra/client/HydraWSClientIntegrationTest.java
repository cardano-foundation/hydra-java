package org.cardanofoundation.hydra.client;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.response.GreetingsResponse;
import org.cardanofoundation.hydra.client.model.query.response.Response;
import org.cardanofoundation.hydra.client.utils.SLF4JHydraLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.client.HydraDevNetwork.*;
import static org.cardanofoundation.hydra.client.model.HydraState.*;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class HydraWSClientIntegrationTest {

    /**
     * Integration tests which tests library and the whole journey from:
     * connection, HydraState.Init -> HydraState.Open -> HydraState.Closed -> HydraState.FanoutPossible -> HydraState.Final
     * and finally closing the connection.
     */
    @Test
    public void testHydraNetworkReachesOpenState() throws InterruptedException {
        var stopWatch = Stopwatch.createStarted();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(getHydraApiUrl(hydraDevNetwork.aliceHydraContainer, HYDRA_ALICE_REMOTE_PORT))
                    .build());
            aliceHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "alice"));

            var errorFuture = new CompletableFuture<Response>();

            var aliceGreetingsFuture = new CompletableFuture<GreetingsResponse>();
            var bobGreetingsFuture = new CompletableFuture<GreetingsResponse>();

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

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(getHydraApiUrl(hydraDevNetwork.bobHydraContainer, HYDRA_BOB_REMOTE_PORT))
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
                    .until(() -> aliceGreetingsFuture, greetingsResponseCompletableFuture -> {

                        try {
                            return greetingsResponseCompletableFuture.isDone()
                                    && greetingsResponseCompletableFuture.get().getHeadStatus() == Idle
                                    && greetingsResponseCompletableFuture.get().getSnapshotUtxo().isEmpty();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });

            log.info("Check if greetings message was set to bob and contains hydra state and contains empty utxo snapshot...");
            await().atMost(Duration.ofMinutes(1))
                    .until(() -> bobGreetingsFuture, greetingsResponseCompletableFuture -> {
                        try {
                            return greetingsResponseCompletableFuture.isDone()
                                    && greetingsResponseCompletableFuture.get().getHeadStatus() == Idle
                                    && greetingsResponseCompletableFuture.get().getSnapshotUtxo().isEmpty();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
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

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(getHydraApiUrl(hydraDevNetwork.aliceHydraContainer, HYDRA_ALICE_REMOTE_PORT))
                    .build());
            aliceHydraWSClient.connectBlocking(1, MINUTES);
            aliceHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "alice"));

            aliceHydraWSClient.init();

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(getHydraApiUrl(hydraDevNetwork.bobHydraContainer, HYDRA_BOB_REMOTE_PORT))
                    .build());
            bobHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "bob"));
            bobHydraWSClient.connectBlocking(1, MINUTES);

            log.info("Awaiting for HydraState[Initializing] from alice hydra node..");
            await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState())
                    .isEqualTo(Initializing));

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState())
                    .isEqualTo(Initializing));

            log.info("Alice is aborting...");
            aliceHydraWSClient.abort();

            log.info("Awaiting for HydraState[Final] from alice hydra node..");
            await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState())
                    .isEqualTo(Final));

            log.info("Awaiting for HydraState[Final] from bob hydra node..");
            await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState())
                    .isEqualTo(Final));

            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();
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
