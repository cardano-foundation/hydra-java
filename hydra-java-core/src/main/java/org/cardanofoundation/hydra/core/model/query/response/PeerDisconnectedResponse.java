package org.cardanofoundation.hydra.core.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.utils.MoreJson;

import java.time.LocalDateTime;

// A peer has disconnected from the head network.
@Getter
@ToString(callSuper = true)
public class PeerDisconnectedResponse extends Response {

    private final String peer;

    private final LocalDateTime timestamp;

    public PeerDisconnectedResponse(String peer, int seq, LocalDateTime timestamp) {
        super(Tag.PeerDisconnected, seq);
        this.peer = peer;
        this.timestamp = timestamp;
    }

    public static PeerDisconnectedResponse create(JsonNode raw) {
        val peer = raw.get("peer").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new PeerDisconnectedResponse(peer, seq, timestamp);
    }

}
