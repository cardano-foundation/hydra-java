package org.cardanofoundation.hydra.client;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.cardano.client.lib.submit.HttpCardanoTxSubmissionService;
import org.cardanofoundation.hydra.cardano.client.lib.params.HydraNodeProtocolParametersAdapter;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.JacksonClasspathSecretKeyCardanoOperatorSupplier;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.GreetingsResponse;
import org.cardanofoundation.hydra.core.model.query.response.Response;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.cardanofoundation.hydra.test.HydraDevNetwork;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.cardano.client.lib.utils.TransactionSigningUtil.sign;
import static org.cardanofoundation.hydra.core.model.HydraState.*;
import static org.cardanofoundation.hydra.core.utils.HexUtils.decodeHexString;
import static org.cardanofoundation.hydra.test.HydraDevNetwork.getTxSubmitWebUrl;

@Slf4j
public class HydraWSClientIntegrationTest1 {

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

            var httpClient = HttpClient.newHttpClient();

            var txSubmitWebUrl = getTxSubmitWebUrl(hydraDevNetwork.getTxSubmitContainer());
            log.info("Tx submit web url: {}", txSubmitWebUrl);
            var txSubmissionClient = new HttpCardanoTxSubmissionService(httpClient, txSubmitWebUrl);

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

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiWebSocketUrl(aliceHydraContainer))
                    .utxoStore(new InMemoryUTxOStore())
                    .snapshotUtxo(true)
                    .build());
            SLF4JHydraLogger aliceHydraLogger = SLF4JHydraLogger.of(log, "alice");
            aliceHydraWSClient.addHydraQueryEventListener(aliceHydraLogger);
            aliceHydraWSClient.addHydraStateEventListener(aliceHydraLogger);

            var aliceHydraWebClient = new HydraWebClient(HttpClient.newHttpClient(), HydraDevNetwork.getHydraApiWebUrl(aliceHydraContainer));
            var bobHydraWebClient = new HydraWebClient(HttpClient.newHttpClient(), HydraDevNetwork.getHydraApiWebUrl(bobHydraContainer));

            var protocolParamsSupplier = new HydraNodeProtocolParametersAdapter(aliceHydraWebClient.fetchProtocolParameters());

            var errorFuture = new CompletableFuture<Response>();

            var aliceState = new AtomicReference<HydraState>();
            var bobState = new AtomicReference<HydraState>();

            var aliceGreetingsFuture = new CompletableFuture<GreetingsResponse>().orTimeout(1, MINUTES);
            var bobGreetingsFuture = new CompletableFuture<GreetingsResponse>().orTimeout(1, MINUTES);

            aliceHydraWSClient.addHydraStateEventListener((prevState, newState) -> aliceState.set(newState));

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

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiWebSocketUrl(bobHydraContainer))
                    .utxoStore(new InMemoryUTxOStore())
                    .snapshotUtxo(true)
                    .build());
            SLF4JHydraLogger bobHydraLogger = SLF4JHydraLogger.of(log, "bob");
            bobHydraWSClient.addHydraQueryEventListener(bobHydraLogger);
            bobHydraWSClient.addHydraStateEventListener(bobHydraLogger);

            bobHydraWSClient.addHydraStateEventListener((prevState, newState) -> bobState.set(newState));

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
                    .until(() -> aliceState.get() == Initializing);

            log.info("Awaiting for HydraState[Initializing] from bob hydra node..");
            await().atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> bobState.get() == Initializing);

            var aliceUtxo = new UTXO();
            aliceUtxo.setAddress("addr_test1vp5cxztpc6hep9ds7fjgmle3l225tk8ske3rmwr9adu0m6qchmx5z");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(100 * 1_000_000)));

            var bobUtxo = new UTXO();
            bobUtxo.setAddress("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh");
            bobUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(100 * 1_000_000)));

            var aliceUtxoMap = Map.of("c8870bcd3602a685ba24b567ae5fec7ecab8500798b427d3d2f04605db1ea9fb#0", aliceUtxo);
            var bobUtxoMap = Map.of("2af765b516d9d99777333029e9abfb4d2bfe462df9c6a8366a4bd11a8ec8d4bd#0", bobUtxo);

            var aliceHeadCommitted = aliceHydraWebClient.commitRequest(aliceUtxoMap);
            var bobHeadCommitted = bobHydraWebClient.commitRequest(bobUtxoMap);

            var aliceCommitTxToSign = aliceHeadCommitted.getCborHex();
            var bobCommitTxToSign = bobHeadCommitted.getCborHex();

            var aliceCommitTxSigned = sign(decodeHexString(aliceCommitTxToSign), aliceOperator.getSecretKey());
            var bobCommitTxSigned = sign(decodeHexString(bobCommitTxToSign), bobOperator.getSecretKey());

            var aliceCommitResult = txSubmissionClient.submitTransaction(aliceCommitTxSigned);
            var bobCommitResult = txSubmissionClient.submitTransaction(bobCommitTxSigned);

            if (!aliceCommitResult.isSuccessful()) {
                log.warn("Alice funds commitment failed, reason: {}", aliceCommitResult.getResponse());
            }

            if (!bobCommitResult.isSuccessful()) {
                log.warn("Bob funds commitment failed, reason: {}", bobCommitResult.getResponse());
            }

            Assertions.assertTrue(aliceCommitResult.isSuccessful());
            Assertions.assertTrue(bobCommitResult.isSuccessful());

            log.info("Checking if alice will see head is open...");
            await()
                    .atMost(Duration.ofMinutes(1))
                     .failFast(errorFuture::isDone)
                    .until(() -> aliceState.get() == Open);

            log.info("Checking if bob will see head is open...");
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> bobState.get() == Open);

            log.info("Bob decided to close the head...");
            bobHydraWSClient.closeHead();

            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> aliceState.get() == Closed);
            await()
                    .atMost(Duration.ofMinutes(1))
                    .failFast(errorFuture::isDone)
                    .until(() -> bobState.get() == Closed);

            log.info("Now we need to wait a few minutes for contestation deadline...");
            await().atMost(Duration.ofMinutes(10))
                    .failFast(errorFuture::isDone)
                    .until(() -> aliceState.get() == FanoutPossible);

            await().atMost(Duration.ofMinutes(10))
                    .failFast(errorFuture::isDone)
                    .until(() -> bobState.get() == FanoutPossible);

            // it is enough that any participant fans out, bob fans out
            bobHydraWSClient.fanOut();

            await()
                    .failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(10))
                    .until(() -> aliceState.get() == Final);
            await()
                    .failFast(errorFuture::isDone)
                    .atMost(Duration.ofMinutes(10))
                    .until(() -> bobState.get() == Final);

            aliceHydraWSClient.closeBlocking();
            bobHydraWSClient.closeBlocking();
        }

        stopWatch.stop();

        log.info("Total exec time: {} seconds", stopWatch.elapsed(SECONDS));
    }

}
