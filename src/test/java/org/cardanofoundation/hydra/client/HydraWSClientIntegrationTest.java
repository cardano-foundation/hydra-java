package org.cardanofoundation.hydra.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.query.response.GreetingsResponse;
import org.cardanofoundation.hydra.client.model.query.response.SnapshotConfirmed;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HydraWSClientIntegrationTest {

    private HydraWSClient hydraWSClient;

    private final String wsUrl = "ws://dev.cf-hydra-voting-poc.metadata.dev.cf-deployments.org:4001";

    @Test
    void testWithHistory() throws InterruptedException {
        var hydraOptions = HydraClientOptions.builder(wsUrl)
                .history(true)
                .build();

        val latch = new CountDownLatch(1);

        hydraWSClient = new HydraWSClient(hydraOptions);
        hydraWSClient.addHydraQueryEventListener(response -> {
            log.info("{}", response.getTag());
            if (response instanceof GreetingsResponse) {
                latch.countDown();
            }
        });

        hydraWSClient.addHydraStateEventListener((prev, now) -> {
            log.info("prev:{}, now:{}", prev, now);
        });

        hydraWSClient.connectBlocking(60, SECONDS);

        assertTrue(latch.await(10, MINUTES));
    }

    @Test
    void testWithoutHistory() throws InterruptedException {
        var hydraOptions = HydraClientOptions.builder(wsUrl)
                .history(false)
                .build();

        val latch = new CountDownLatch(1); //

        hydraWSClient = new HydraWSClient(hydraOptions);
        hydraWSClient.addHydraQueryEventListener(response -> {
            log.info("{}", response);

            if (response instanceof SnapshotConfirmed) {
                log.info("{}", ((SnapshotConfirmed) response).getSnapshot().getConfirmedTransactions());
            }

            if (response instanceof GreetingsResponse) {
                latch.countDown();
            }
        });

        hydraWSClient.addHydraStateEventListener((prev, now) -> {
            log.info("prev:{}, now:{}", prev, now);
        });

        hydraWSClient.connectBlocking(60, SECONDS);

        assertTrue(latch.await(10, MINUTES));
        assertEquals(HydraState.Idle, hydraWSClient.getHydraState());
    }

    @AfterAll
    void terminate() throws InterruptedException {
        hydraWSClient.closeBlocking();
    }

}
