package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

// A peer has disconnected from the head network.
public class PeerDisconnectedResponse extends Response {

    private final String peer;

    private final int seq;

    private final LocalDateTime timestamp;

    public PeerDisconnectedResponse(String peer, int seq, LocalDateTime timestamp) {
        super(Tag.PeerDisconnected);
        this.peer = peer;
        this.seq = seq;
        this.timestamp = timestamp;
    }

    public String getPeer() {
        return peer;
    }

    public int getSeq() {
        return seq;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public static PeerDisconnectedResponse create(JsonNode raw) {
        val peer = raw.get("peer").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new PeerDisconnectedResponse(peer, seq, timestamp);
    }

    @Override
    public String toString() {
        return "PeerDisconnected{" +
                "peer='" + peer + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", tag=" + tag +
                '}';
    }

}
