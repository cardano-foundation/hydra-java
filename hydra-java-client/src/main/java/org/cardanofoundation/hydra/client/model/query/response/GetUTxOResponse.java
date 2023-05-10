package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.utils.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// Emitted as a result of a `GetUTxO` to reflect the current UTxO of the underlying node.
@Getter
@ToString(callSuper = true)
public class GetUTxOResponse extends Response {

    private final String headId;

    private final LocalDateTime timestamp;

    private final Map<String, UTXO> utxo;

    public GetUTxOResponse(String headId, int seq, LocalDateTime timestamp, Map<String, UTXO> utxo) {
        super(Tag.GetUTxOResponse, seq);
        this.headId = headId;
        this.timestamp = timestamp;
        this.utxo = utxo;
    }

    public static GetUTxOResponse create(JsonNode raw) {
        val utxo = MoreJson.convertUTxOMap(raw.get("utxo"));

        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new GetUTxOResponse(headId, seq, timestamp, utxo);
    }

}
