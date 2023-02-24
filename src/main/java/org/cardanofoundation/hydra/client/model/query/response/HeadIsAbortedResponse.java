package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Map;

@Getter
// One of the participant did `Abort` the head before all commits were done or collected.
public class HeadIsAbortedResponse extends QueryResponse {

    private final Map<String, UTXO> utxo;

    public HeadIsAbortedResponse(Map<String, UTXO> utxo) {
        super(Tag.HeadIsAborted);
        this.utxo = utxo;
    }

    public static HeadIsAbortedResponse create(JsonNode raw) {
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));

        return new HeadIsAbortedResponse(utxo);
    }

    @Override
    public String toString() {
        return "HeadIsAbortedResponse{" +
                "utxo=" + utxo +
                ", tag=" + tag +
                '}';
    }

}

//{
//        "snapshotNumber": 1,
//        "tag": "HeadIsContested"
//}