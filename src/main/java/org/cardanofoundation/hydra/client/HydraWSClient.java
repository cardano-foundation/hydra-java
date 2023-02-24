package org.cardanofoundation.hydra.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.delegates.Trace;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.*;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.*;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
// not thread safe yet
public class HydraWSClient extends WebSocketClient {

    private Optional<HydraStateEventListener> hydraStateEventListener = Optional.empty();

    private Optional<HydraQueryEventListener> hydraQueryEventListener = Optional.empty();

    private Map<Tag, Function<JsonNode, QueryResponse>> handlers = new HashMap<>();

    private StateMachine<HydraState, HydraTrigger> stateMachine;

    public HydraWSClient(URI serverURI) {
        super(serverURI);
        initStateMachine();
        createResponseHandlers();
    }

    private void initStateMachine() {
        this.stateMachine = new StateMachine<>(HydraState.Unknown, createFSMConfig());
        this.stateMachine.setTrace(new Trace<>() {

            @Override
            public void trigger(HydraTrigger trigger) {}

            @Override
            public void transition(HydraTrigger trigger, HydraState source, HydraState destination) {
                hydraStateEventListener.ifPresent(s -> s.onStateChanged(source, destination));
            }
        });
    }

    private void createResponseHandlers() {
        handlers.put(Tag.TxValid, TxValidResponse::create);
        handlers.put(Tag.CommandFailed, CommandFailedResponse::create);
        handlers.put(Tag.ReadyToCommit, ReadyToCommitResponse::create);
        handlers.put(Tag.HeadIsOpen, HeadIsOpenResponse::create);
        handlers.put(Tag.Committed, CommittedResponse::create);
        handlers.put(Tag.PostTxOnChainFailed, PostChainTxFailedResponse::create);
        handlers.put(Tag.PeerConnected, PeerConnectedResponse::create);
        handlers.put(Tag.PeerDisconnected, PeerDisconnectedResponse::create);
        handlers.put(Tag.Greetings, GreetingsResponse::create);
        handlers.put(Tag.RolledBack, RolledbackResponse::create);
        handlers.put(Tag.TxSeen, TxSeenResponse::create);
        handlers.put(Tag.HeadIsClosed, HeadIsClosedResponse::create);
        handlers.put(Tag.GetUTxOResponse, GetUTxOResponse::create);
    }

    private static StateMachineConfig<HydraState, HydraTrigger> createFSMConfig() {
        val stateMachineConfig = new StateMachineConfig<HydraState, HydraTrigger>();

        stateMachineConfig.configure(HydraState.Unknown)
                .permit(HydraTrigger.Connected, HydraState.Idle);

        stateMachineConfig.configure(HydraState.Idle)
                .permit(HydraTrigger.ReadyToCommit, HydraState.Initializing);

        stateMachineConfig.configure(HydraState.Initializing)
                .permit(HydraTrigger.HeadIsOpen, HydraState.Open);

        return stateMachineConfig;
    }

    public HydraState getHydraState() {
        return stateMachine.getState();
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
        val trigger = HydraTrigger.Connected;
        if (stateMachine.canFire(trigger)) {
            stateMachine.fire(trigger);
        }
    }

    @Override
    public void onMessage(String message) {
        log.debug("Received: {}", message);

        val raw = MoreJson.read(message);
        val tagString = raw.get("tag").asText();
        val tag = Tag.find(tagString);

        if (tag.isEmpty()) {
            log.warn("We don't support tag:{} yet, json:{}", tagString, message);
            return;
        }

        val t = tag.orElseThrow();

        val r = handlers.get(t).apply(raw);

        hydraQueryEventListener.ifPresent(s -> s.onQueryResponse(r));

        if (r.getTag() == Tag.ReadyToCommit) {
            val trigger = HydraTrigger.ReadyToCommit;
            if (stateMachine.canFire(trigger)) {
                stateMachine.fire(trigger);
            }
        }

        if (r.getTag() == Tag.HeadIsOpen) {
            val trigger = HydraTrigger.HeadIsOpen;
            if (stateMachine.canFire(trigger)) {
                stateMachine.fire(trigger);
            }
        }
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
    public boolean init(int period) {
        if (stateMachine.getState() == HydraState.Idle) {
            val request = new InitRequest(period);
            send(request.getRequestBody());
            return true;
        }

        return false;
    }

    // Join an initialized head. This is how parties get to inject funds inside a head. Note however that the utxo is an object and can be empty should a participant wants to join a head without locking any funds.
    public boolean commit(String utxoId, UTXO utxo) {
        if (stateMachine.getState() == HydraState.Initializing) {
            val request = new CommitRequest();
            request.addUTXO(utxoId, utxo);
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

    // Join an initialized head. This is how parties get to inject funds inside a head. Note however that the utxo is an object and can be empty should a participant wants to join a head without locking any funds.
    public boolean commit() {
        if (stateMachine.getState() == HydraState.Initializing) {
            val request = new CommitRequest();
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

    // Submit a transaction through the head. Note that the transaction is only broadcast if well-formed and valid.
    public boolean newTx(String transaction) {
        if (stateMachine.getState() == HydraState.Open) {
            val request = new NewTxRequest(transaction);
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

    // Terminate a head with the latest known snapshot. This effectively moves the head from the Open state to the Close state where the contestation phase begin. As a result of closing a head, no more transactions can be submitted via NewTx.
    public boolean closeHead() {
        if (stateMachine.getState() == HydraState.Open) {
            val request = new CloseHeadRequest();
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

    // Aborts a head before it is opened. This can only be done before all participants have committed. Once opened, the head can't be aborted anymore but it can be closed using: Close.
    public boolean abort() {
        if (stateMachine.getState() == HydraState.Idle) {
            val request = new AbortHeadRequest();
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

    // Asynchronously access the current UTxO set of the Hydra node. This eventually triggers a UTxO event from the server.
    public boolean getUTXO() {
        if (stateMachine.getState() == HydraState.Open) {
            val request = new GetUTxORequest();
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

}
