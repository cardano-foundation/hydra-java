package org.cardanofoundation.hydra.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.*;
import org.cardanofoundation.hydra.client.util.MoreJson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Optional;

@Slf4j
// not thread safe yet
public class HydraWSClient extends WebSocketClient {

    private final static ResponseTagStateMapper RESPONSE_TAG_STATE_MAPPER = new ResponseTagStateMapper();

    private final static ResponseTagHandlers RESPONSE_TAG_HANDLERS = new ResponseTagHandlers();

    private final int fromSeq;

    private Optional<HydraStateEventListener> hydraStateEventListener = Optional.empty();

    private Optional<HydraQueryEventListener> hydraQueryEventListener = Optional.empty();

    private HydraState hydraState = HydraState.Unknown;

    public HydraWSClient(URI serverURI, int fromSeq) {
        super(serverURI);
        this.fromSeq = fromSeq;
        this.hydraState = HydraState.Unknown;
    }

    public HydraWSClient(URI serverURI) {
        this(serverURI, 0);
    }

    public HydraState getHydraState() {
        return hydraState;
    }

    public void setHydraQueryEventListener(HydraQueryEventListener hydraQueryEventListener) {
        this.hydraQueryEventListener = Optional.ofNullable(hydraQueryEventListener);
    }

    public void setHydraStateEventListener(HydraStateEventListener hydraStateEventListener) {
        this.hydraStateEventListener = Optional.ofNullable(hydraStateEventListener);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.info("Connection Established!");
        log.debug("onOpen -> ServerHandshake: {}", serverHandshake);
        hydraState = HydraState.Idle;
    }

    @Override
    public void onMessage(String message) {
        log.debug("Received: {}", message);

        val raw = MoreJson.read(message);
        val tagString = raw.get("tag").asText();
        val seq = raw.get("seq").asInt();

        val maybeTag = Tag.find(tagString);

        if (raw.has("utxo")) {
            val utxo = MoreJson.convertUTxOMap(raw.get("utxo"));

            log.info("utxo's map size:{}", utxo.size());
        }

        if (maybeTag.isEmpty()) {
            log.warn("We don't support tag:{} yet, json:{}", tagString, message);
            return;
        }

        val tag = maybeTag.orElseThrow();
        val maybeResponseHandler = RESPONSE_TAG_HANDLERS.responseHandlerFor(tag);
        if (maybeResponseHandler.isEmpty()) {
            log.error("We don't have response handler for the following tag:{}", tag);
        }
        val responseHandler = maybeResponseHandler.orElseThrow();
        val queryResponse = responseHandler.apply(raw);

        if (isSeqAccepted(seq)) {
            hydraQueryEventListener.ifPresent(hydraQueryEventListener -> hydraQueryEventListener.onResponse(queryResponse));
        }

        RESPONSE_TAG_STATE_MAPPER.stateForTag(tag).ifPresent(newHydraState -> {
            val prevState = hydraState;
            this.hydraState = newHydraState;
            if (isSeqAccepted(seq)) {
                hydraStateEventListener.ifPresent(l -> l.onStateChanged(prevState, hydraState));
            }
        });
    }

    private boolean isSeqAccepted(int seq) {
        return seq >= fromSeq;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Connection closed by {}, Code: {}{}", (remote ? "remote peer" : "client"), code,
                (reason == null || reason.isEmpty()) ? reason : ", Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        log.error("websocket error: {}", ex.getMessage());
    }

    // Initializes a new Head. This command is a no-op when a Head is already open and the server will output an CommandFailed message should this happen.
    public void init() {
        val request = new InitRequest();
        send(request.getRequestBody());
    }

    // Aborts a head before it is opened. This can only be done before all participants have committed. Once opened, the head can't be aborted anymore but it can be closed using: Close.
    public void abort() {
        val request = new AbortHeadRequest();
        send(request.getRequestBody());
    }

    // Join an initialized head. This is how parties get to inject funds inside a head. Note however that the utxo is an object and can be empty should a participant wants to join a head without locking any funds.
    public void commit(String utxoId, UTXO utxo) {
        val request = new CommitRequest();
        request.addUTXO(utxoId, utxo);
        send(request.getRequestBody());
    }

    // Join an initialized head. This is how parties get to inject funds inside a head. Note however that the utxo is an object and can be empty should a participant wants to join a head without locking any funds.
    public void commit() {
        val request = new CommitRequest();
        send(request.getRequestBody());
    }

    // Submit a transaction through the head. Note that the transaction is only broadcast if well-formed and valid.
    public void newTx(String transaction) {
        val request = new NewTxRequest(transaction);
        send(request.getRequestBody());
    }

    // Terminate a head with the latest known snapshot. This effectively moves the head from the Open state to the Close state where the contestation phase begin. As a result of closing a head, no more transactions can be submitted via NewTx.
    public void closeHead() {
        val request = new CloseHeadRequest();
        send(request.getRequestBody());
    }

    // Challenge the latest snapshot announced as a result of a head closure from another participant. Note that this necessarily contest with the latest snapshot known of your local Hydra node. Participants can only contest once.
    public void contest() {
        val request = new ContestHeadRequest();
        send(request.getRequestBody());
    }


    // Finalize a head after the contestation period passed. This will distribute the final (as closed and maybe contested) head state back on the layer 1.
    public void fanOut() {
        val request = new FanoutRequest();
        send(request.getRequestBody());
    }

    // Asynchronously access the current UTxO set of the Hydra node. This eventually triggers a UTxO event from the server.
    public void getUTXO() {
        val request = new GetUTxORequest();
        send(request.getRequestBody());
    }

}
