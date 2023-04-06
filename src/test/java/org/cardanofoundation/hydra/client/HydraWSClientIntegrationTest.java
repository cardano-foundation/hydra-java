package org.cardanofoundation.hydra.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class HydraWSClientIntegrationTest {

    private HydraWSClient hydraWSClient;

    @BeforeAll
    void initClient() throws InterruptedException, URISyntaxException {
        hydraWSClient = new HydraWSClient(new URI("ws://dev.cf-hydra-voting-poc.metadata.dev.cf-deployments.org:4001"));
        //hydraWSClient.setHydraQueryEventListener(response -> log.info("response:{}", response));
        hydraWSClient.setHydraStateEventListener((prev, now) -> log.info("prev:{}, now:{}", prev, now));
        hydraWSClient.connectBlocking(60, TimeUnit.SECONDS);
    }

    @Test
    void init() throws InterruptedException {
        val l = new CountDownLatch(10);

        l.await(10, TimeUnit.SECONDS);
    }

    @AfterAll
    void terminate() throws InterruptedException {
        hydraWSClient.closeBlocking();
    }

}
