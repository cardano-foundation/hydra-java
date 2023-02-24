package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Map;

// Emitted as a result of a `GetUTxO` to reflect the current UTxO of the underlying node.
public class GetUTxOResponse extends Response {

    private final Map<String, UTXO> utxo;

    public GetUTxOResponse(Map<String, UTXO> utxo) {
        super(Tag.GetUTxOResponse);
        this.utxo = utxo;
    }

    public static GetUTxOResponse create(JsonNode raw) {
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));

        return new GetUTxOResponse(utxo);
    }

    @Override
    public String toString() {
        return "GetUTxO{" +
                "utxo=" + utxo +
                ", tag=" + tag +
                '}';
    }

    //    {
//        "tag": "GetUTxOResponse",
//        "utxo": {
//        "09d34606abdcd0b10ebc89307cbfa0b469f9144194137b45b7a04b273961add8#687": {
//            "address": "addr1w9htvds89a78ex2uls5y969ttry9s3k9etww0staxzndwlgmzuul5",
//                    "value": {
//                "lovelace": 7620669
//            }
//        }
//    },
//    }

}
