package org.cardanofoundation.hydra.reactor;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.cardano.client.lib.submit.HttpCardanoTxSubmissionService;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.JsonClasspathWalletSupplierFactory;
import org.cardanofoundation.hydra.core.model.query.response.HeadIsAbortedResponse;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.cardanofoundation.hydra.test.HydraDevNetwork;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.core.model.HydraState.*;
import static org.cardanofoundation.hydra.core.model.Tag.Greetings;
import static org.cardanofoundation.hydra.core.model.Tag.HeadIsAborted;
import static org.cardanofoundation.hydra.test.HydraDevNetwork.getTxSubmitWebUrl;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class HydraReactiveClientIntegrationTest2 {

    private final static Network NETWORK = Networks.testnet();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Here we will test the following things:
     * - connecting to the head
     * - alice sends init command (getting Hydra in the intializing state)
     * - bob decides to abort
     * - head reaches aborted final
     */
    @Test
    public void test() throws InterruptedException, CborSerializationException, IOException {
        var stopWatch = Stopwatch.createStarted();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var txSubmitWebUrl = getTxSubmitWebUrl(hydraDevNetwork.getTxSubmitContainer());
            log.info("Tx submit web url: {}", txSubmitWebUrl);
            var txSubmissionClient = new HttpCardanoTxSubmissionService(HttpClient.newHttpClient(), txSubmitWebUrl);

            var nodeSocketPath = hydraDevNetwork.getRemoteCardanoLocalSocketPath();
            log.info("Node socket path: {}", nodeSocketPath);

            var aliceWallet = new JsonClasspathWalletSupplierFactory(
                    "devnet/credentials/alice-funds.sk",
                    "devnet/credentials/alice-funds.vk",
                    objectMapper).loadWallet()
                    .getWallet();

            var bobWallet = new JsonClasspathWalletSupplierFactory(
                    "devnet/credentials/bob-funds.sk",
                    "devnet/credentials/bob-funds.vk",
                    objectMapper).loadWallet()
                    .getWallet();


            var aliceHydraContainer = hydraDevNetwork.getAliceHydraContainer();
            var bobHydraContainer = hydraDevNetwork.getBobHydraContainer();

            var aliceHydraReactiveClient = new HydraReactiveClient(new InMemoryUTxOStore(), HydraDevNetwork.getHydraApiWebSocketUrl(aliceHydraContainer));
            var bobHydraReactiveClient = new HydraReactiveClient(new InMemoryUTxOStore(), HydraDevNetwork.getHydraApiWebSocketUrl(bobHydraContainer));

            var aliceGreetingsResponse = aliceHydraReactiveClient.openConnection()
                    .block();

            var bobGreetingsResponse = bobHydraReactiveClient.openConnection()
                    .block();

            assertNotNull(aliceGreetingsResponse);
            assertNotNull(bobGreetingsResponse);

            Assertions.assertEquals(Idle, aliceGreetingsResponse.getHeadStatus());
            Assertions.assertEquals(Idle, bobGreetingsResponse.getHeadStatus());

            Assertions.assertTrue(aliceGreetingsResponse.getHydraNodeVersion().startsWith("0.13."));
            Assertions.assertTrue(bobGreetingsResponse.getHydraNodeVersion().startsWith("0.13."));

            Assertions.assertEquals(Greetings, aliceGreetingsResponse.getTag());
            Assertions.assertEquals(Greetings, bobGreetingsResponse.getTag());

            Assertions.assertTrue(aliceGreetingsResponse.getSnapshotUtxo().isEmpty());
            Assertions.assertTrue(bobGreetingsResponse.getSnapshotUtxo().isEmpty());

            log.info("Alice is sending init to the head... (only one actor needs to do it)");

            var headIsInitializingResponse = aliceHydraReactiveClient.initHead()
                    .block();

            assertNotNull(headIsInitializingResponse);

            Assertions.assertEquals(2, headIsInitializingResponse.getParties().size());

            await().atMost(30, SECONDS)
                    .until(() -> aliceHydraReactiveClient.getHydraState() == Initializing);

            await().atMost(30, SECONDS)
                    .until(() -> bobHydraReactiveClient.getHydraState() == Initializing);

            Assertions.assertEquals(Initializing, aliceHydraReactiveClient.getHydraState());
            Assertions.assertEquals(Initializing, bobHydraReactiveClient.getHydraState());


            log.info("Bob decided to abort...");
            HeadIsAbortedResponse headIsClosedResponse = bobHydraReactiveClient.abortHead()
                    .block();

            Assertions.assertEquals("9fab302be6225ff2e1c23298aa74412ae0160b6f2839316403006c9e", headIsClosedResponse.getHeadId());
            Assertions.assertEquals(HeadIsAborted, headIsClosedResponse.getTag());

            // nobody committed anything
            Assertions.assertTrue(headIsClosedResponse.getUtxo().isEmpty());

            await().atMost(30, SECONDS)
                    .until(() -> aliceHydraReactiveClient.getHydraState() == Final);

            await().atMost(30, SECONDS)
                    .until(() -> bobHydraReactiveClient.getHydraState() == Final);

            Assertions.assertEquals(Final, aliceHydraReactiveClient.getHydraState());
            Assertions.assertEquals(Final, bobHydraReactiveClient.getHydraState());

            aliceHydraReactiveClient.closeConnection();
            bobHydraReactiveClient.closeConnection();
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

}
