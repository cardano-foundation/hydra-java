package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.utils.MoreJson;

import java.time.LocalDateTime;

//   The contestation period has passed and the head can now be finalized by
//   a fanout transaction.
@Getter
@ToString(callSuper = true)
public class ReadyToFanoutResponse extends Response {

    private final String headId;

    private final LocalDateTime timestamp;

    public ReadyToFanoutResponse(String headId, int seq, LocalDateTime timestamp) {
        super(Tag.ReadyToFanout, seq);
        this.headId = headId;
        this.timestamp = timestamp;
    }

    public static ReadyToFanoutResponse create(JsonNode raw) {
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new ReadyToFanoutResponse(headId, seq, timestamp);
    }

}

