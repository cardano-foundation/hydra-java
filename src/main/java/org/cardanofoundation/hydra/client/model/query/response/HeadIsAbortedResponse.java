package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// One of the participant did `Abort` the head before all commits were done or collected.
public class HeadIsAbortedResponse extends Response {

    private final String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    private final Map<String, UTXO> utxo;

    public HeadIsAbortedResponse(String headId, int seq, LocalDateTime timestamp, Map<String, UTXO> utxo) {
        super(Tag.HeadIsAborted);
        this.headId = headId;
        this.seq = seq;
        this.timestamp = timestamp;
        this.utxo = utxo;
    }

    public static HeadIsAbortedResponse create(JsonNode raw) {
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsAbortedResponse(headId, seq, timestamp, utxo);
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

    public Map<String, UTXO> getUtxo() {
        return utxo;
    }

    @Override
    public String toString() {
        return "HeadIsAborted{" +
                "headId='" + headId + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", utxo=" + utxo +
                ", tag=" + tag +
                '}';
    }
}

//{
//        "snapshotNumber": 1,
//        "tag": "HeadIsContested"
//}