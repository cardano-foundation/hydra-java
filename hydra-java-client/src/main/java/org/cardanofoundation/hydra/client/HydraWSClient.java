package org.cardanofoundation.hydra.client;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.query.request.*;
import org.cardanofoundation.hydra.core.model.query.response.FailureResponse;
import org.cardanofoundation.hydra.core.model.query.response.GreetingsResponse;
import org.cardanofoundation.hydra.core.store.UTxOStore;
import org.cardanofoundation.hydra.core.utils.MoreJson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Slf4j
// not thread safe yet
public class HydraWSClient {

    private final static ResponseTagStateMapper RESPONSE_TAG_STATE_MAPPER = new ResponseTagStateMapper();

    private final ResponseTagHandlers responseTagHandlers;

    private final HydraWebSocketHandler hydraWebSocketHandler;

    private final List<HydraStateEventListener> hydraStateEventListeners = new CopyOnWriteArrayList<>();

    private final List<HydraQueryEventListener> hydraQueryEventListeners = new CopyOnWriteArrayList<>();

    private final HydraClientOptions hydraClientOptions;

    @Getter
    private final UTxOStore utxoStore;

    @Getter
    private HydraState hydraState;


    public HydraWSClient(HydraClientOptions hydraClientOptions) {
        final URI hydraServerUri = createHydraServerUri(hydraClientOptions);
        log.info("hydra connection url:{}", hydraServerUri);
        this.hydraWebSocketHandler = new HydraWebSocketHandler(hydraServerUri);
        this.hydraClientOptions = hydraClientOptions;
        this.hydraState = HydraState.Unknown;
        this.utxoStore = hydraClientOptions.getUtxoStore();
        this.responseTagHandlers = new ResponseTagHandlers(utxoStore);
    }

    /**
     * Returns true if the websocket connection is open.
     * @return
     */
    public boolean isOpen() {
        return hydraWebSocketHandler.isOpen();
    }

    /**
     * Returns true if the websocket connection is closed.
     *
     * @return true if the websocket connection is closed
     */
    public boolean isClosed() {
        return hydraWebSocketHandler.isClosed();
    }

    /**
     * Returns true if the websocket connection is closing.
     *
     * @return true if the websocket connection is closing
     */
    public boolean isClosing() {
        return hydraWebSocketHandler.isClosing();
    }


    /**
     * Add a HydraQueryEventListener instance to the list of listeners.
     *
     * @param eventListener - listener to add
     * @return this instance
     */
    public HydraWSClient addHydraQueryEventListener(HydraQueryEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraQueryEventListener instance cannot be null!");
        }

        hydraQueryEventListeners.add(eventListener);

        return this;
    }

    /**
     * Add a HydraStateEventListener instance to the list of listeners.
     *
     * @param eventListener - listener to add
     * @return this instance
     */
    public HydraWSClient addHydraStateEventListener(HydraStateEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraStateEventListener instance cannot be null!");
        }

        hydraStateEventListeners.add(eventListener);

        return this;
    }

    /**
     * Remove a HydraQueryEventListener instance from the list of listeners.
     * @param eventListener - listener to remove
     * @return this instance
     */
    public HydraWSClient removeHydraQueryEventListener(HydraQueryEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraQueryEventListener instance cannot be null!");
        }

        hydraQueryEventListeners.remove(eventListener);

        return this;
    }

    /**
     * Remove a HydraStateEventListener instance from the list of listeners.
     *
     * @param eventListener - listener to remove
     * @return this instance
     */
    public HydraWSClient removeHydraStateEventListener(HydraStateEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraStateEventListener instance cannot be null!");
        }

        hydraStateEventListeners.remove(eventListener);

        return this;
    }

    /**
     * Remove all HydraQueryEventListener instances from the list of listeners.
     */
    public void clearAllHydraQueryEventListeners() {
        hydraQueryEventListeners.clear();
    }

    /**
     * Remove all HydraStateEventListener instances from the list of listeners.
     */
    public void clearAllHydraStateEventListeners() {
        hydraStateEventListeners.clear();
    }

    /**
     * Connect to the hydra server.
     */
    public void connect() {
        hydraWebSocketHandler.connect();
    }

    /**
     * Connect to the hydra server and block until the connection is established.
     * @throws InterruptedException
     */
    public void connectBlocking() throws InterruptedException {
        hydraWebSocketHandler.connectBlocking();
    }

    /**
     * Connect to the hydra server and block until the connection is established or the timeout is reached.
     * @param time
     * @param timeUnit
     * @throws InterruptedException
     */
    public void connectBlocking(int time, TimeUnit timeUnit) throws InterruptedException {
        hydraWebSocketHandler.connectBlocking(time, timeUnit);
    }

    /**
     * Close the websocket connection.
     */
    public void close() {
        hydraWebSocketHandler.close();
    }

    /**
     * Close the websocket connection with the code and message
     *
     * @param code - code to close the websocket connection with
     * @param message - message to pass on the websocket connection closing
     */
    public void close(int code, String message) {
        hydraWebSocketHandler.close(code, message);
    }

    /**
     * Close the websocket connection and block until the connection is closed.
     */
    public void closeBlocking() throws InterruptedException {
        hydraWebSocketHandler.closeBlocking();
    }

    private static URI createHydraServerUri(HydraClientOptions hydraClientOptions) {
        String serverURI = hydraClientOptions.getServerURI();
        if (!serverURI.startsWith("ws://") && !serverURI.startsWith("wss://")) {
            throw new IllegalArgumentException("Invalid web socket urlPath:" + serverURI);
        }

        if (serverURI.endsWith("?")) {
            return URI.create(serverURI);
        }

        var delim = "&";

        var urlPath = new StringJoiner(delim)
                .add(format("history=%s", (hydraClientOptions.isHistory() ? "yes" : "no")))
                .add(format("snapshot-utxo=%s", (hydraClientOptions.isSnapshotUtxo() ? "yes" : "no")))
                .add(format("tx-output=%s", hydraClientOptions.getTransactionFormat().name().toLowerCase()))
                .toString();

        return URI.create(format("%s?%s", serverURI, urlPath));
    }

    /**
     * Initializes a new Hydra head.
     * This command is a no-op when a Head is already open and the server will output an CommandFailed message should this happen.
     */
    public void init() {
        val request = new InitRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Aborts already initialized Hydra head before it is opened.
     *
     * This can only be done BEFORE all participants have committed. Once opened, the head can't be aborted anymore, but it can be closed using:
     * close head request instead.
     */
    public void abort() {
        val request = new AbortHeadRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Join an initialized Hydra head and commit a single UTXO.
     *
     * This is how parties get to inject funds inside a head. Note, however,
     * that the utxo is an object and can be empty should a participant wants to join a head without locking any funds.
     *
     * Note that once all Hydra head participants commit the hydra head will open.
     */
    public void commit(String utxoId, UTXO utxo) {
        val request = new CommitRequest();
        request.addUTXO(utxoId, utxo);
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Join an initialized Hydra head and commit multiple UTXOs.
     *
     * Note that once all Hydra head participants commit the hydra head will open.
     * @param utxoMap
     */
    public void commit(Map<String, UTXO> utxoMap) {
        val request = new CommitRequest();
        utxoMap.forEach(request::addUTXO);
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     ** Join an initialized Hydra head and commit no UTXOs.
     *
     * Note that once all Hydra head participants commit the hydra head will open.
     */
    public void commit() {
        val request = new CommitRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Submit a transaction through the head. Note that the transaction is only broadcast among all Hydra head
     * participants if well-formed and valid.
     *
     * You should expect to get either <code>TransactionValidResponse</code> or <code>TransactionInvalidResponse</code>
     */
    public void submitTx(String transaction) {
        val request = new NewTxRequest(transaction);
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Terminate a Hydra head with the latest known snapshot.
     * This request effectively moves the head from the <em>Open</em> state to the <em>Close</em> state where the contestation phase begin.
     *
     * As a result of closing a head, no more transactions can be submitted to the Hydra network via <em>NewTx</em> request.
     */
    public void closeHead() {
        val request = new CloseHeadRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Challenge the latest snapshot announced as a result of a Hydra head closure from another participant.
     *
     * Note that this necessarily contest with the latest snapshot known of your local Hydra node.
     * Participants can only contest once.
     */
    public void contest() {
        val request = new ContestHeadRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Finalize a Hydra head after the contestation period passed.
     *
     * This will distribute the final (as closed and maybe contested) head state back on the Cardano's layer 1.
     */
    public void fanOut() {
        val request = new FanoutRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    /**
     * Asynchronously access the current UTxO set of the Hydra node.
     *
     * This eventually triggers a response with all UTxOs (last known snapshot) from the server.
     */
    public void getUTXO() {
        val request = new GetUTxORequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    private class HydraWebSocketHandler extends WebSocketClient {

        public HydraWebSocketHandler(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            log.info("Connection Established!");
            log.debug("onOpen -> ServerHandshake: {}", serverHandshake);
            HydraWSClient.this.hydraState = HydraState.Unknown;
        }

        @Override
        public void onMessage(String message) {
            log.debug("Received: {}", message);

            val raw = MoreJson.read(message);
            val tagString = raw.get("tag").asText();

            val maybeTag = Tag.find(tagString);

            if (maybeTag.isEmpty()) {
                log.warn("We don't support tag:{} yet, json:{}", tagString, message);
                return;
            }

            val tag = maybeTag.orElseThrow();

            val maybeResponseHandler = responseTagHandlers.responseHandlerFor(tag);
            if (maybeResponseHandler.isEmpty()) {
                log.error("We don't have response handler for the following tag:{}", tag);
            }
            val responseHandler = maybeResponseHandler.orElseThrow();
            val queryResponse = responseHandler.apply(raw);

            // if we don't have history this means we need to use Greetings message to get hydra state data
            if (!hydraClientOptions.isHistory() && tag == Tag.Greetings) {
                val greetingsResponse = (GreetingsResponse) queryResponse;
                fireHydraStateChanged(HydraWSClient.this.hydraState, greetingsResponse.getHeadStatus());
            } else {
                RESPONSE_TAG_STATE_MAPPER.stateForTag(tag).ifPresent(newHydraState -> {
                    fireHydraStateChanged(HydraWSClient.this.hydraState, newHydraState);
                });
            }

            if (queryResponse instanceof FailureResponse failureResponse) {
                if (hydraClientOptions.isDoNotPropagateLowLevelFailures() && failureResponse.isLowLevelFailure()) {
                    log.warn("Low level consensus failure, ignoring...");
                    return;
                }
            }

            var listeners = List.copyOf(hydraQueryEventListeners);
            listeners.forEach(hydraQueryEventListener -> hydraQueryEventListener.onResponse(queryResponse));
            if (queryResponse.isFailure()) {
                listeners.forEach(hydraQueryEventListener -> hydraQueryEventListener.onFailure(queryResponse));
            } else {
                listeners.forEach(hydraQueryEventListener -> hydraQueryEventListener.onSuccess(queryResponse));
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            String formattedActor = remote ? "remote peer" : "client";
            String formattedReason = (reason == null || reason.isEmpty()) ? reason : ", Reason: " + reason;

            log.info("Connection closed by {}, Code: {}{}", formattedActor, code, formattedReason);
        }

        @Override
        public void onError(Exception e) {
            if (e == null) {
                log.error("Hydra websocket error: null");
                return;
            }
            log.error("Hydra websocket error: {}", e.getMessage());
        }
    }

    private void fireHydraStateChanged(HydraState currentState, HydraState newState) {
        if (currentState == newState) {
            return;
        }
        HydraWSClient.this.hydraState = newState;

        List.copyOf(hydraStateEventListeners).forEach(l -> l.onStateChanged(currentState, newState));
    }

}
