package org.cardanofoundation.hydra.client;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.response.GreetingsResponse;
import org.cardanofoundation.hydra.core.model.query.response.Response;
import org.cardanofoundation.hydra.core.store.InMemoryUTxOStore;
import org.cardanofoundation.hydra.test.HydraDevNetwork;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.cardanofoundation.hydra.core.model.HydraState.*;

@Slf4j
public class HydraWSClientIntegrationTest1 {

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
    public void test() throws InterruptedException {
        var stopWatch = Stopwatch.createStarted();

        try (HydraDevNetwork hydraDevNetwork = new HydraDevNetwork()) {
            hydraDevNetwork.start();

            var aliceHydraContainer = hydraDevNetwork.getAliceHydraContainer();
            var bobHydraContainer = hydraDevNetwork.getBobHydraContainer();

            var aliceHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(aliceHydraContainer))
                    .utxoStore(new InMemoryUTxOStore())
                    .snapshotUtxo(true)
                    .build());
            SLF4JHydraLogger aliceHydraLogger = SLF4JHydraLogger.of(log, "alice");
            aliceHydraWSClient.addHydraQueryEventListener(aliceHydraLogger);
            aliceHydraWSClient.addHydraStateEventListener(aliceHydraLogger);

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

            var bobHydraWSClient = new HydraWSClient(HydraClientOptions.builder(HydraDevNetwork.getHydraApiUrl(bobHydraContainer))
                    .utxoStore(new InMemoryUTxOStore())
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
            aliceUtxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
            aliceUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000 * 1_000_000)));

            var bobUtxo = new UTXO();
            bobUtxo.setAddress("addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k");
            bobUtxo.setValue(Map.of("lovelace", BigInteger.valueOf(500 * 1_000_000)));

            aliceHydraWSClient.commit("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0", aliceUtxo);
            bobHydraWSClient.commit("db982e0b69fb742188e45feedfd631bbce6738884d266356868efb9907e10cf9#0", bobUtxo);

            log.info("Checking if alice and bob will see head is open...");
            await()
                    .atMost(Duration.ofMinutes(1))
                     .failFast(errorFuture::isDone)
                    .until(() -> aliceState.get() == Open);
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

            // it is enough that any participant fans out
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
