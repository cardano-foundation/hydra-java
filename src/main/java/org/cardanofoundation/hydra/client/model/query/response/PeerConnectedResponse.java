package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

// A peer is now connected to the head network.
@Getter
@ToString(callSuper = true)
public class PeerConnectedResponse extends Response {

    private final String peer;

    private final LocalDateTime timestamp;

    public PeerConnectedResponse(String peer, int seq, LocalDateTime timestamp) {
        super(Tag.PeerConnected, seq);
        this.peer = peer;
        this.timestamp = timestamp;
    }

    public static PeerConnectedResponse create(JsonNode raw) {
        val peer = raw.get("peer").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new PeerConnectedResponse(peer, seq, timestamp);
    }

}
