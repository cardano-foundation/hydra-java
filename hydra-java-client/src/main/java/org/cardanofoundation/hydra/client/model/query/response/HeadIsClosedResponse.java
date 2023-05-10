package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.utils.MoreJson;

import java.time.LocalDateTime;

// A `Close` transaction has been observed on-chain, the head is now closed and the contestation phase begins.
@Getter
@ToString(callSuper = true)
public class HeadIsClosedResponse extends Response {

    private final int snapshotNumber;

    private final LocalDateTime contestationDeadline;

    private final String headId;

    private final LocalDateTime timestamp;

    public HeadIsClosedResponse(String headId,
                                int snapshotNumber,
                                LocalDateTime contestationDeadline,
                                int seq,
                                LocalDateTime timestamp) {
        super(Tag.HeadIsClosed, seq);
        this.headId = headId;
        this.snapshotNumber = snapshotNumber;
        this.contestationDeadline = contestationDeadline;
        this.timestamp = timestamp;
    }

    public static HeadIsClosedResponse create(JsonNode raw) {
        val snapshotNumber = raw.get("snapshotNumber").asInt();
        val contestationDeadline = MoreJson.convert(raw.get("contestationDeadline"), LocalDateTime.class);
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsClosedResponse(headId, snapshotNumber, contestationDeadline, seq, timestamp);
    }

}
