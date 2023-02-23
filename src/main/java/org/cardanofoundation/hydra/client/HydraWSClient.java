package org.cardanofoundation.hydra.client;

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

import static org.cardanofoundation.hydra.client.model.query.request.base.Tag.NewTx;

@Slf4j
// not thread safe yet
public class HydraWSClient extends WebSocketClient {

    private HydraStateEventListener hydraStateEventListener;

    private HydraQueryEventListener hydraQueryEventListener;

    private HydraState hydraState = HydraState.Unknown;

    private ResponseDeserializer responseDeserializer = new ResponseDeserializer();

    public HydraWSClient(URI serverURI) {
        super(serverURI);
    }

    public HydraState getHydraState() {
        return hydraState;
    }

    public void setHydraQueryEventListener(HydraQueryEventListener hydraQueryEventListener) {
        this.hydraQueryEventListener = hydraQueryEventListener;
    }

    public void setHydraStateEventListener(HydraStateEventListener hydraStateEventListener) {
        this.hydraStateEventListener = hydraStateEventListener;
    }

    public void setResponseDeserializer(ResponseDeserializer responseDeserializer) {
        this.responseDeserializer = responseDeserializer;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        log.info("Connection Established!");
        log.debug("onOpen -> ServerHandshake: {}", serverHandshake);
        if (hydraStateEventListener != null) {
            hydraStateEventListener.onStateChanged(HydraState.Unknown, HydraState.Unknown);
        }
    }

    @Override
    public void onMessage(String message) {
        log.debug("Received: {}", message);
        val response = responseDeserializer.deserialize(message);

        response.ifPresent(qr -> {
            val prev = this.hydraState;

            if (hydraQueryEventListener != null) {
                hydraQueryEventListener.onQueryResponse(qr);
            }
            if (hydraState == HydraState.Idle && qr.getTag() == Tag.ReadyToCommit) {
                this.hydraState = HydraState.Initializing;
            }
            if (qr.getTag() == Tag.HeadIsOpen) {
                this.hydraState = HydraState.Open;
            }
            if (qr.getTag() == Tag.Greetings) {
                this.hydraState = HydraState.Idle;
            }
            // TODO further state changes
            if (hydraStateEventListener != null && prev != this.hydraState) {
                hydraStateEventListener.onStateChanged(prev, hydraState);
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

    public void init(int period) {
        val request = new InitRequest(period);
        send(request.getRequestBody());
    }

    public void commit(String utxoId, UTXO utxo) {
        val request = new CommitRequest();
        request.addUTXO(utxoId, utxo);
        send(request.getRequestBody());
    }

    public void newTx(String transaction) {
//        if (hydraState == HydraState.Open) {
            val request = new NewTxRequest(transaction);
            send(request.getRequestBody());
//        }
    }
}
