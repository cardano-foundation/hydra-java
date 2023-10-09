package org.cardanofoundation.hydra.client;

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

@Slf4j
// not thread safe yet
public class HydraWSClient {

    private final static ResponseTagStateMapper RESPONSE_TAG_STATE_MAPPER = new ResponseTagStateMapper();

    private final ResponseTagHandlers responseTagHandlers;

    protected final HydraWebSocketHandler hydraWebSocketHandler;

    private final List<HydraStateEventListener> hydraStateEventListeners = new CopyOnWriteArrayList<>();

    private final List<HydraQueryEventListener> hydraQueryEventListeners = new CopyOnWriteArrayList<>();

    private final HydraClientOptions hydraClientOptions;

    private final UTxOStore utxoStore;

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

    public UTxOStore getUtxoStore() {
        return utxoStore;
    }

    public HydraState getHydraState() {
        return hydraState;
    }

    public boolean isOpen() {
        return hydraWebSocketHandler.isOpen();
    }

    public boolean isClosed() {
        return hydraWebSocketHandler.isClosed();
    }

    public boolean isClosing() {
        return hydraWebSocketHandler.isClosing();
    }

    public HydraWSClient addHydraQueryEventListener(HydraQueryEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraQueryEventListener instance cannot be null!");
        }

        hydraQueryEventListeners.add(eventListener);

        return this;
    }

    public HydraWSClient addHydraStateEventListener(HydraStateEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraStateEventListener instance cannot be null!");
        }

        hydraStateEventListeners.add(eventListener);

        return this;
    }

    public HydraWSClient removeHydraQueryEventListener(HydraQueryEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraQueryEventListener instance cannot be null!");
        }

        hydraQueryEventListeners.remove(eventListener);

        return this;
    }

    public void clearAllHydraQueryEventListeners() {
        hydraQueryEventListeners.clear();
    }

    public void clearAllHydraStateEventListeners() {
        hydraStateEventListeners.clear();
    }

    public HydraWSClient removeHydraStateEventListener(HydraStateEventListener eventListener) {
        if (eventListener == null) {
            throw new IllegalArgumentException("HydraStateEventListener instance cannot be null!");
        }

        hydraStateEventListeners.remove(eventListener);

        return this;
    }

    public void connect() {
        hydraWebSocketHandler.connect();
    }

    public void connectBlocking() throws InterruptedException {
        hydraWebSocketHandler.connectBlocking();
    }

    public void connectBlocking(int time, TimeUnit timeUnit) throws InterruptedException {
        hydraWebSocketHandler.connectBlocking(time, timeUnit);
    }

    public void close() {
        hydraWebSocketHandler.close();
    }

    public void close(int code, String message) {
        hydraWebSocketHandler.close(code, message);
    }

    public void closeBlocking() throws InterruptedException {
        hydraWebSocketHandler.closeBlocking();
    }

    private static URI createHydraServerUri(HydraClientOptions hydraClientOptions) {
        String serverURI = hydraClientOptions.getServerURI();
        if (!serverURI.startsWith("ws://") && !serverURI.startsWith("wss://")) {
            throw new IllegalArgumentException("Invalid web socket url:" + serverURI);
        }

        if (serverURI.endsWith("?")) {
            return URI.create(serverURI);
        }

        var transactionFormat = hydraClientOptions.getTransactionFormat();

        var delim = "&";
        var joiner = new StringJoiner(delim)
                .add(String.format("history=%s", (hydraClientOptions.isHistory() ? "yes" : "no")))
                .merge(transactionFormat == null ? new StringJoiner(delim) : new StringJoiner(delim).add(String.format("tx-output=%s", transactionFormat.name().toLowerCase())));

        return URI.create(String.format("%s?%s", serverURI, joiner));
    }

    // Initializes a new Head. This command is a no-op when a Head is already open and the server will output an CommandFailed message should this happen.
    public void init() {
        val request = new InitRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Aborts a head before it is opened. This can only be done before all participants have committed. Once opened, the head can't be aborted anymore but it can be closed using: Close.
    public void abort() {
        val request = new AbortHeadRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Join an initialized head. This is how parties get to inject funds inside a head. Note however that the utxo is an object and can be empty should a participant wants to join a head without locking any funds.
    public void commit(String utxoId, UTXO utxo) {
        val request = new CommitRequest();
        request.addUTXO(utxoId, utxo);
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    public void commit(Map<String, UTXO> utxoMap) {
        val request = new CommitRequest();
        utxoMap.forEach(request::addUTXO);
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Join an initialized head. This is how parties get to inject funds inside a head. Note however that the utxo is an object and can be empty should a participant wants to join a head without locking any funds.
    public void commit() {
        val request = new CommitRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Submit a transaction through the head. Note that the transaction is only broadcast if well-formed and valid.
    public void submitTx(String transaction) {
        val request = new NewTxRequest(transaction);
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Terminate a head with the latest known snapshot. This effectively moves the head from the Open state to the Close state where the contestation phase begin. As a result of closing a head, no more transactions can be submitted via NewTx.
    public void closeHead() {
        val request = new CloseHeadRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Challenge the latest snapshot announced as a result of a head closure from another participant. Note that this necessarily contest with the latest snapshot known of your local Hydra node. Participants can only contest once.
    public void contest() {
        val request = new ContestHeadRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Finalize a head after the contestation period passed. This will distribute the final (as closed and maybe contested) head state back on the layer 1.
    public void fanOut() {
        val request = new FanoutRequest();
        hydraWebSocketHandler.send(request.getRequestBody());
    }

    // Asynchronously access the current UTxO set of the Hydra node. This eventually triggers a UTxO event from the server.
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

            if (queryResponse instanceof FailureResponse) {
                var failureResponse = (FailureResponse) queryResponse;
                if (hydraClientOptions.isDoNotPropagateLowLevelFailures() && failureResponse.isLowLevelFailure()) {
                    log.info("Low level consensus failure, ignoring...");
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
            log.error("Hydra websocket error: {}", e.getMessage());
        }
    }

    private boolean fireHydraStateChanged(HydraState currentState, HydraState newState) {
        if (currentState == newState) {
            return false;
        }
        HydraWSClient.this.hydraState = newState;

        List.copyOf(hydraStateEventListeners).forEach(l -> l.onStateChanged(currentState, newState));

        return true;
    }

}
