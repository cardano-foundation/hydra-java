package org.cardanofoundation.hydra.core.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.utils.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// The head was already closed and the contestation period is now over.
@Getter
@ToString(callSuper = true)
public class HeadIsFinalizedResponse extends Response {

    private final String headId;

    private final LocalDateTime timestamp;

    private final Map<String, UTXO> utxo;

    public HeadIsFinalizedResponse(String headId, int seq, LocalDateTime timestamp, Map<String, UTXO> utxo) {
        super(Tag.HeadIsFinalized, seq);
        this.headId = headId;
        this.timestamp = timestamp;
        this.utxo = utxo;
    }

    public static HeadIsFinalizedResponse create(JsonNode raw) {
        val utxo = MoreJson.convertUTxOMap(raw.get("utxo"));
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsFinalizedResponse(headId, seq, timestamp, utxo);
    }

}

