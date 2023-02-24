package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Map;

@Getter
// The head was already closed and the contestation period is now over.
public class HeadIsFinalizedResponse extends Response {

    private final Map<String, UTXO> utxo;

    public HeadIsFinalizedResponse(Map<String, UTXO> utxo) {
        super(Tag.HeadIsFinalized);
        this.utxo = utxo;
    }

    public static HeadIsFinalizedResponse create(JsonNode raw) {
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));

        return new HeadIsFinalizedResponse(utxo);
    }

    @Override
    public String toString() {
        return "HeadIsFinalizedResponse{" +
                "utxo=" + utxo +
                ", tag=" + tag +
                '}';
    }

}

