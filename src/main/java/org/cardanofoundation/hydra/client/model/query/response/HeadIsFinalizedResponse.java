package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// The head was already closed and the contestation period is now over.
@Getter
@ToString(callSuper = true)
public class HeadIsFinalizedResponse extends Response {

    private final String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    private final Map<String, UTXO> utxo;

    public HeadIsFinalizedResponse(String headId, int seq, LocalDateTime timestamp, Map<String, UTXO> utxo) {
        super(Tag.HeadIsFinalized);
        this.headId = headId;
        this.seq = seq;
        this.timestamp = timestamp;
        this.utxo = utxo;
    }

    public static HeadIsFinalizedResponse create(JsonNode raw) {
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsFinalizedResponse(headId, seq, timestamp, utxo);
    }

}

