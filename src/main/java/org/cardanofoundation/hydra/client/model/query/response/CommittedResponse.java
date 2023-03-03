package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// A `Commit` from a head participant has been observed on-chain.
@Getter
@ToString(callSuper = true)
public class CommittedResponse extends Response {

    private final Party party;
    private final Map<String, UTXO> utxo;

    private final int seq;

    private final LocalDateTime timestamp;

    public CommittedResponse(Party party, Map<String, UTXO> utxo, int seq, LocalDateTime timestamp) {
        super(Tag.Committed);
        this.party = party;
        this.utxo = utxo;
        this.seq = seq;
        this.timestamp = timestamp;
    }

    public static CommittedResponse create(JsonNode raw) {
        val party = MoreJson.convert(raw.get("party"), Party.class);
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new CommittedResponse(party, utxo, seq, timestamp);
    }

}
