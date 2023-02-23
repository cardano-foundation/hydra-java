package org.cardanofoundation.hydra.client;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.delegates.Trace;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.CommitRequest;
import org.cardanofoundation.hydra.client.model.query.request.InitRequest;
import org.cardanofoundation.hydra.client.model.query.request.NewTxRequest;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Optional;

@Slf4j
// not thread safe yet
public class HydraWSClient extends WebSocketClient {

    private Optional<HydraStateEventListener> hydraStateEventListener = Optional.empty();

    private Optional<HydraQueryEventListener> hydraQueryEventListener = Optional.empty();

    private ResponseDeserializer responseDeserializer = new ResponseDeserializer();

    private StateMachine<HydraState, HydraTrigger> stateMachine;

    public HydraWSClient(URI serverURI) {
        super(serverURI);

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
        val response = responseDeserializer.deserialize(message);

        response.ifPresent(qr -> {
            hydraQueryEventListener.ifPresent(s -> s.onQueryResponse(qr));

            if (qr.getTag() == Tag.ReadyToCommit) {
                val trigger = HydraTrigger.ReadyToCommit;
                if (stateMachine.canFire(trigger)) {
                    stateMachine.fire(trigger);
                }
            }
            if (qr.getTag() == Tag.HeadIsOpen) {
                val trigger = HydraTrigger.HeadIsOpen;
                if (stateMachine.canFire(trigger)) {
                    stateMachine.fire(trigger);
                }
            }
        });
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

    public boolean init(int period) {
        if (stateMachine.getState() == HydraState.Idle) {
            val request = new InitRequest(period);
            send(request.getRequestBody());
            return true;
        }

        return false;
    }

    public boolean commit(String utxoId, UTXO utxo) {
        if (stateMachine.getState() == HydraState.Initializing) {
            val request = new CommitRequest();
            request.addUTXO(utxoId, utxo);
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

    public boolean newTx(String transaction) {
        if (stateMachine.getState() == HydraState.Open) {
            val request = new NewTxRequest(transaction);
            send(request.getRequestBody());

            return true;
        }

        return false;
    }

}
