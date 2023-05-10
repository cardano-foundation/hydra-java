package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.utils.MoreJson;

import java.time.LocalDateTime;
import java.util.List;

// A `Init` transaction has been observed on-chain by the given party who's now ready to commit into the initialized head.
@Getter
@ToString(callSuper = true)
public class HeadIsInitializingResponse extends Response {

    private final List<Party> parties;

    private final String headId;

    private final LocalDateTime timestamp;

    public HeadIsInitializingResponse(String headId, List<Party> parties, int seq, LocalDateTime timestamp) {
        super(Tag.HeadIsInitializing, seq);
        this.headId = headId;
        this.parties = parties;
        this.timestamp = timestamp;
    }

    public static HeadIsInitializingResponse create(JsonNode raw) {
        val utxoNode = raw.get("parties");
        val parties = MoreJson.convertPartiesList(utxoNode);
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsInitializingResponse(headId, parties, seq, timestamp);
    }

}
