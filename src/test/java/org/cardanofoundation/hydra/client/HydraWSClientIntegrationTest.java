package org.cardanofoundation.hydra.client;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.helpers.SLF4JHydraLogger;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
            aliceHydraWSClient.connectBlocking(1, MINUTES);
            aliceHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "alice"));

            var errorFuture = new CompletableFuture<Response>();

            aliceHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
               }
            });

            aliceHydraWSClient.init();

            var bobFailures = new CompletableFuture<Response>();
            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(getHydraApiUrl(hydraDevNetwork.bobHydraContainer, HYDRA_BOB_REMOTE_PORT))
                    .build());
            bobHydraWSClient.addHydraQueryEventListener(SLF4JHydraLogger.of(log, "bob"));
            bobHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
                @Override
                public void onFailure(Response response) {
                    bobFailures.complete(response);
                }
            });

            bobHydraWSClient.connectBlocking(1, MINUTES);

            log.info("Awaiting for HydraState[Initializing] from alice hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Initializing));

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(bobFailures::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Initializing));

            var aliceUtxo = new UTXO();
            aliceUtxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000 * 1_000_000)));

            aliceHydraWSClient.commit("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0", aliceUtxo);
            //aliceHydraWSClient.commit("61b102f21bbc88bb8d4fb0df7bac0658ad48e34e62c8f81f415c00d0d7b19ba8#0", aliceUtxo);
            bobHydraWSClient.commit();

            await()
                    .atMost(Duration.ofMinutes(1))
                     .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Open));
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(bobFailures::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Open));

            bobHydraWSClient.closeHead();

            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Closed));
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(bobFailures::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(Closed));

            log.info("Now we need to wait a few minutes for contestation deadline...");

            await().atMost(Duration.ofMinutes(10))
                    .failFast(errorFuture::isDone)
                    .untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(FanoutPossible));

            await().atMost(Duration.ofMinutes(10))
                    .failFast(bobFailures::isDone)
                    .untilAsserted(() -> assertThat(bobHydraWSClient.getHydraState()).isEqualTo(FanoutPossible));

            // it is enough that any participant fans out
            bobHydraWSClient.fanOut();

            await()
                    .failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(10)).untilAsserted(() -> assertThat(aliceHydraWSClient.getHydraState()).isEqualTo(Final));
            await()
                    .failFast(bobFailures::isDone)
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

        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

}