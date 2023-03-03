package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

// A `Close` transaction has been observed on-chain, the head is now closed and the contestation phase begins.
public class HeadIsClosedResponse extends Response {

    private final int snapshotNumber;

    private final LocalDateTime contestationDeadline;

    private final String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    public HeadIsClosedResponse(String headId,
                                int snapshotNumber,
                                LocalDateTime contestationDeadline,
                                int seq,
                                LocalDateTime timestamp) {
        super(Tag.HeadIsClosed);
        this.headId = headId;
        this.snapshotNumber = snapshotNumber;
        this.contestationDeadline = contestationDeadline;
        this.seq = seq;
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

    public int getSnapshotNumber() {
        return snapshotNumber;
    }

    public LocalDateTime getContestationDeadline() {
        return contestationDeadline;
    }

    public String getHeadId() {
        return headId;
    }

    public int getSeq() {
        return seq;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "HeadIsClosed{" +
                "snapshotNumber=" + snapshotNumber +
                ", contestationDeadline=" + contestationDeadline +
                ", headId='" + headId + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", tag=" + tag +
                '}';
    }

}

//{
//        "contestationDeadline": "2022-11-07T13:08:22Z",
//        "snapshotNumber": 1,
//        "tag": "HeadIsClosed"
//}