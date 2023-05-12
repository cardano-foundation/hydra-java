package org.cardanofoundation.hydra.client.client;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.HydraClientOptions;
import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.client.HydraWSClient;
import org.cardanofoundation.hydra.client.SLF4JHydraLogger;
import org.cardanofoundation.hydra.client.client.helpers.HydraDevNetwork;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.query.response.Response;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.core.model.HydraState.Final;
import static org.cardanofoundation.hydra.core.model.HydraState.Initializing;

@Slf4j
public class HydraWSClientIntegrationTest2 {

    /**
     * Here we will test the following things:
     * - connecting to the head
     * - alice sends init command (getting Hydra in the intializing state)
     * - bob decides to abort
     * - head reaches aborted state
     */
    @Test
    public void testHydraNetworkReachesAbortState() throws InterruptedException {
        var stopWatch = Stopwatch.createStarted();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var errorFuture = new CompletableFuture<Response>();
            var aliceState = new AtomicReference<HydraState>();
            var bobState = new AtomicReference<HydraState>();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getAliceHydraContainer()))
                    .withUTxOStore(new InMemoryUTxOStore())
                    .build());
            SLF4JHydraLogger aliceHydraLogger = SLF4JHydraLogger.of(log, "alice");
            aliceHydraWSClient.addHydraQueryEventListener(aliceHydraLogger);
            aliceHydraWSClient.addHydraStateEventListener(aliceHydraLogger);
            aliceHydraWSClient.addHydraStateEventListener((prevState, newState) -> aliceState.set(newState));

            aliceHydraWSClient.addHydraQueryEventListener(new HydraQueryEventListener.Stub() {
                @Override
                public void onFailure(Response response) {
                    errorFuture.complete(response);
                }
            });

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(hydraDevNetwork.getBobHydraContainer()))
                    .withUTxOStore(new InMemoryUTxOStore())
                    .build());

            SLF4JHydraLogger bobHydraLogger = SLF4JHydraLogger.of(log, "bob");
            bobHydraWSClient.addHydraQueryEventListener(bobHydraLogger);
            bobHydraWSClient.addHydraStateEventListener(bobHydraLogger);

            bobHydraWSClient.addHydraStateEventListener((prevState, newState) -> bobState.set(newState));

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
                    .atMost(Duration.ofMinutes(1))
                    .until(() -> aliceState.get() == Initializing);

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(1))
                    .until(() -> bobState.get() == Initializing);

            log.info("Bob is aborting...");
            bobHydraWSClient.abort();

            log.info("Awaiting for HydraState[Final] from alice hydra node..");
            await().failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(1))
                    .until(() -> aliceState.get() == Final);

            log.info("Awaiting for HydraState[Final] from bob hydra node..");
            await().failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(1))
                    .until(() -> aliceState.get() == Final);

            log.info("Closing alice and bob connections...");
            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

}
