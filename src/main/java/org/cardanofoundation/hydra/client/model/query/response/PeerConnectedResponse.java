package org.cardanofoundation.hydra.client.model.query.response;

import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

public class PeerConnectedResponse extends QueryResponse {

    private final String peer;

    public PeerConnectedResponse(String peer) {
        super(Tag.PeerConnected);
        this.peer = peer;
    }

    public String getPeer() {
        return peer;
    }

    @Override
    public String toString() {
        return "PeerConnected{" +
                "peer='" + peer + '\'' +
                ", tag=" + tag +
                '}';
    }
}
