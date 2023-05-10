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

// All parties have committed, and a successful `CollectCom` transaction was observed on-chain. The head is now open; transactions can flow.
@Getter
@ToString(callSuper = true)
public class HeadIsOpenResponse extends Response {

    private final String headId;

    private final Map<String, UTXO> utxo;

    private final LocalDateTime timestamp;

    public HeadIsOpenResponse(String headId,
                              Map<String, UTXO> utxo,
                              int seq, LocalDateTime timestamp) {
        super(Tag.HeadIsOpen, seq);
        this.headId = headId;
        this.utxo = utxo;
        this.timestamp = timestamp;
    }

    public static HeadIsOpenResponse create(JsonNode raw) {
        val utxoMap = MoreJson.convertUTxOMap(raw.get("utxo"));
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsOpenResponse(headId, utxoMap, seq, timestamp);
    }

}
