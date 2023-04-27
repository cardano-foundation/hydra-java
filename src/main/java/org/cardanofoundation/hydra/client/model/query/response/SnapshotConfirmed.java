package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Snapshot;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

// A `Init` transaction has been observed on-chain by the given party who's now ready to commit into the initialized head.
@Getter
@ToString(callSuper = true)
public class SnapshotConfirmed extends Response {

    private final String headId;

    private final LocalDateTime timestamp;

    private final Snapshot snapshot;

    public SnapshotConfirmed(String headId,
                             int seq,
                             LocalDateTime timestamp,
                             Snapshot snapshot) {
        super(Tag.SnapshotConfirmed, seq);
        this.headId = headId;
        this.timestamp = timestamp;
        this.snapshot = snapshot;
    }

    public static SnapshotConfirmed create(JsonNode raw) {
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);
        val snapshot = MoreJson.convert(raw.get("snapshot"), Snapshot.class);

        return new SnapshotConfirmed(headId, seq, timestamp, snapshot);
    }

}
