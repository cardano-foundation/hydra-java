package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

// A peer is now connected to the head network.
public class PeerConnectedResponse extends QueryResponse {

    private final String peer;

    public PeerConnectedResponse(String peer) {
        super(Tag.PeerConnected);
        this.peer = peer;
    }

    public String getPeer() {
        return peer;
    }

    public static PeerConnectedResponse create(JsonNode raw) {
        val peer = raw.get("peer").asText();

        return new PeerConnectedResponse(peer);
    }

    @Override
    public String toString() {
        return "PeerConnected{" +
                "peer='" + peer + '\'' +
                ", tag=" + tag +
                '}';
    }
}
