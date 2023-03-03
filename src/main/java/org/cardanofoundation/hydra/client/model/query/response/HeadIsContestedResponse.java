package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

//        A `Contest` transaction has been observed on-chain, that means the head
//        state has been successfully contested and given snapshot number is now
//        the latest accepted snapshot.
@Getter
@ToString(callSuper = true)
public class HeadIsContestedResponse extends Response {

    private final String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    private final int snapshotNumber;

    public HeadIsContestedResponse(String headId, int seq, LocalDateTime timestamp, int snapshotNumber) {
        super(Tag.HeadIsClosed);
        this.headId = headId;
        this.seq = seq;
        this.timestamp = timestamp;
        this.snapshotNumber = snapshotNumber;
    }

    public static HeadIsContestedResponse create(JsonNode raw) {
        val snapshotNumber = raw.get("snapshotNumber").asInt();
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsContestedResponse(headId, seq, timestamp, snapshotNumber);
    }


}
