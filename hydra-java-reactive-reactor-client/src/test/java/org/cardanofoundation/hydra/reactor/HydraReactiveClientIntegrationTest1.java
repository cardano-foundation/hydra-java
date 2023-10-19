package org.cardanofoundation.hydra.reactor;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.cardano.client.lib.submit.HttpCardanoTxSubmissionService;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.JacksonClasspathSecretKeyCardanoOperatorSupplier;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.HeadIsClosedResponse;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.cardanofoundation.hydra.test.HydraDevNetwork;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.cardano.client.lib.utils.TransactionSigningUtil.sign;
import static org.cardanofoundation.hydra.core.model.HydraState.*;
import static org.cardanofoundation.hydra.core.model.Tag.Greetings;
import static org.cardanofoundation.hydra.core.utils.HexUtils.decodeHexString;
import static org.cardanofoundation.hydra.test.HydraDevNetwork.getTxSubmitWebUrl;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class HydraReactiveClientIntegrationTest1 {

    private final static Network NETWORK = Networks.testnet();

    /**
     * Here we will test the following things:
     * - connect operators to the head using separate web socket connections
     * - both hydra head operators getting greetings response
     * - checking that greetings message contains empty snapshot
     * - starting initialising and checking that hydra state becomes Initialising
     * - both bob and alice committing funds to the head
     * - checking that both alice and bob funded are recognised to be committed by the network (HeadIsOpen state)
     * - one operator (bob) decides to close the head and waiting for head is closed state
     * - all operators wait for contestation period which leads to FanOutPossible state
     * - one operator fans out...
     * - Head reaches final state
     */
    @Test
    public void test() throws InterruptedException, CborSerializationException {
        var stopWatch = Stopwatch.createStarted();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var txSubmitWebUrl = getTxSubmitWebUrl(hydraDevNetwork.getTxSubmitContainer());
            log.info("Tx submit web url: {}", txSubmitWebUrl);
            var txSubmissionClient = new HttpCardanoTxSubmissionService(HttpClient.newHttpClient(), txSubmitWebUrl);

            var nodeSocketPath = hydraDevNetwork.getRemoteCardanoLocalSocketPath();
            log.info("Node socket path: {}", nodeSocketPath);

            var aliceOperator = new JacksonClasspathSecretKeyCardanoOperatorSupplier(
                    "devnet/credentials/alice-funds.sk",
                    NETWORK).getOperator();

            var bobOperator = new JacksonClasspathSecretKeyCardanoOperatorSupplier(
                    "devnet/credentials/bob-funds.sk",
                    NETWORK)
                    .getOperator();

            var aliceHydraContainer = hydraDevNetwork.getAliceHydraContainer();
            var bobHydraContainer = hydraDevNetwork.getBobHydraContainer();

            var aliceHydraWebClient = new HydraReactiveWebClient(HttpClient.newHttpClient(), HydraDevNetwork.getHydraApiWebUrl(aliceHydraContainer));
            var bobHydraWebClient = new HydraReactiveWebClient(HttpClient.newHttpClient(), HydraDevNetwork.getHydraApiWebUrl(bobHydraContainer));

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

            var aliceUtxo = new UTXO();
            aliceUtxo.setAddress("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(100 * 1_000_000)));

            var bobUtxo = new UTXO();
            bobUtxo.setAddress("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh");
            bobUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(100 * 1_000_000)));

            var aliceUtxoMap = Map.of("c8870bcd3602a685ba24b567ae5fec7ecab8500798b427d3d2f04605db1ea9fb#0", aliceUtxo);
            var bobUtxoMap = Map.of("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0", bobUtxo);

            var aliceHeadCommitted = aliceHydraWebClient.commitRequest(aliceUtxoMap).block();
            var bobHeadCommitted = bobHydraWebClient.commitRequest(bobUtxoMap).block();

            assertNotNull(aliceHeadCommitted);
            assertNotNull(bobHeadCommitted);

            var aliceCommitTxToSign = aliceHeadCommitted.getCborHex();
            var bobCommitTxToSign = bobHeadCommitted.getCborHex();

            var aliceCommitTxSigned = sign(decodeHexString(aliceCommitTxToSign), aliceOperator.getSecretKey());
            var bobCommitTxSigned = sign(decodeHexString(bobCommitTxToSign), bobOperator.getSecretKey());

            var aliceCommitResult = txSubmissionClient.submitTransaction(aliceCommitTxSigned);
            var bobCommitResult = txSubmissionClient.submitTransaction(bobCommitTxSigned);

            if (!aliceCommitResult.isSuccessful()) {
                fail("Alice funds commitment failed, reason: " + aliceCommitResult.getResponse());
            }

            if (!bobCommitResult.isSuccessful()) {
                fail("Bob funds commitment failed, reason: " + bobCommitResult.getResponse());
            }

            log.info("Checking if alice will see head is open...");

            await().atMost(30, SECONDS)
                    .until(() -> aliceHydraReactiveClient.getHydraState() == Open);

            await().atMost(30, SECONDS)
                    .until(() -> bobHydraReactiveClient.getHydraState() == Open);

            Assertions.assertEquals(Open, aliceHydraReactiveClient.getHydraState());
            Assertions.assertEquals(Open, bobHydraReactiveClient.getHydraState());

            log.info("Bob decided to close the head...");
            HeadIsClosedResponse headIsClosedResponse = bobHydraReactiveClient.closeHead()
                    .block();

            Assertions.assertEquals(0, headIsClosedResponse.getSnapshotNumber());

            await().atMost(30, SECONDS)
                    .until(() -> aliceHydraReactiveClient.getHydraState() == Closed);

            await().atMost(30, SECONDS)
                    .until(() -> bobHydraReactiveClient.getHydraState() == Closed);

            Assertions.assertEquals(Closed, aliceHydraReactiveClient.getHydraState());
            Assertions.assertEquals(Closed, bobHydraReactiveClient.getHydraState());

            log.info("Now we need to wait a few minutes for contestation deadline...");

            await().atMost(Duration.ofMinutes(10))
                    .until(() -> aliceHydraReactiveClient.getHydraState() == FanoutPossible);

            await().atMost(Duration.ofMinutes(10))
                    .until(() -> bobHydraReactiveClient.getHydraState() == FanoutPossible);

            Assertions.assertEquals(FanoutPossible, aliceHydraReactiveClient.getHydraState());
            Assertions.assertEquals(FanoutPossible, bobHydraReactiveClient.getHydraState());

//            // it is enough that any participant fans out, bob fans out
            aliceHydraReactiveClient.fanOutHead()
                    .block();

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
