package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;

// A peer has disconnected from the head network.
public class PeerDisconnectedResponse extends Response {

    private final String peer;

    public PeerDisconnectedResponse(String peer) {
        super(Tag.PeerDisconnected);
        this.peer = peer;
    }

    public String getPeer() {
        return peer;
    }

    public static PeerDisconnectedResponse create(JsonNode raw) {
        val peer = raw.get("peer").asText();

        return new PeerDisconnectedResponse(peer);
    }

    @Override
    public String toString() {
        return "PeerDisconnected{" +
                "peer='" + peer + '\'' +
                ", tag=" + tag +
                '}';
    }

}
