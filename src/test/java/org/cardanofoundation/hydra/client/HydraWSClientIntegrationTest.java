package org.cardanofoundation.hydra.client;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.client.HydraDevNetwork.*;
import static org.cardanofoundation.hydra.client.model.HydraState.Initializing;
import static org.cardanofoundation.hydra.client.model.HydraState.Open;

@Slf4j
public class HydraWSClientIntegrationTest {

    @Test
    void testHydraNetworkReachesOpenState() throws InterruptedException {
        var stopWatch = Stopwatch.createStarted();
        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(getHydraApiUrl(hydraDevNetwork.aliceHydraContainer, HYDRA_ALICE_REMOTE_PORT))
                    .build());
            aliceHydraWSClient.connectBlocking(1, MINUTES);
            aliceHydraWSClient.addHydraQueryEventListener(response -> log.info("[alice]:" + response.toString()));

            aliceHydraWSClient.init();

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(getHydraApiUrl(hydraDevNetwork.bobHydraContainer, HYDRA_BOB_REMOTE_PORT))
                    .build());
            bobHydraWSClient.addHydraQueryEventListener(response -> log.info("[bob]:" + response.toString()));
            bobHydraWSClient.connectBlocking(1, MINUTES);

            log.info("Awaiting for HydraState[Initializing] from alice hydra node..");
            await().atMost(Duration.ofMinutes(1)).until(() -> aliceHydraWSClient.getHydraState() == Initializing);

            log.info("Awaiting for HydraState[Initializing] from alice hydra node..");
            await().atMost(Duration.ofMinutes(1)).until(() -> bobHydraWSClient.getHydraState() == Initializing);

            log.info("Alice hydra state:{}", aliceHydraWSClient.getHydraState());
            log.info("Bob hydra state:{}", bobHydraWSClient.getHydraState());

            assertThat(aliceHydraWSClient.getHydraState())
                    .isEqualTo(Initializing);

            assertThat(bobHydraWSClient.getHydraState())
                    .isEqualTo(Initializing);

            var aliceUtxo = new UTXO();
            aliceUtxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000 * 1_000_000)));

            aliceHydraWSClient.commit("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0", aliceUtxo);
            bobHydraWSClient.commit();

            await().atMost(Duration.ofMinutes(1)).until(() -> aliceHydraWSClient.getHydraState() == Open);
            await().atMost(Duration.ofMinutes(1)).until(() -> bobHydraWSClient.getHydraState() == Open);

            log.info("Alice hydra state:{}", aliceHydraWSClient.getHydraState());
            log.info("Bob hydra state:{}", bobHydraWSClient.getHydraState());

            assertThat(aliceHydraWSClient.getHydraState())
                    .isEqualTo(Open);

            assertThat(bobHydraWSClient.getHydraState())
                    .isEqualTo(Open);
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

}