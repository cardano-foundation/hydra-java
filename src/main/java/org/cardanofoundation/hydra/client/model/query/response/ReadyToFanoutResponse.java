package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

//   The contestation period has passed and the head can now be finalized by
//   a fanout transaction.
public class ReadyToFanoutResponse extends Response {

    private final String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    public ReadyToFanoutResponse(String headId, int seq, LocalDateTime timestamp) {
        super(Tag.ReadyToFanout);
        this.headId = headId;
        this.seq = seq;
        this.timestamp = timestamp;
    }

    public static ReadyToFanoutResponse create(JsonNode raw) {
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new ReadyToFanoutResponse(headId, seq, timestamp);
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
        return "ReadyToFanout{" +
                "headId='" + headId + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", tag=" + tag +
                '}';
    }

}

//{
//        "tag": "ReadyToFanout"
//}