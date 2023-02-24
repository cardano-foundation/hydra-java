package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Map;

@Getter
public class HeadIsOpenResponse extends QueryResponse {

    private Map<String, UTXO> utxo;

    public HeadIsOpenResponse(Map<String, UTXO> utxo) {
        super(Tag.HeadIsOpen);
        this.utxo = utxo;
    }

    public Map<String, UTXO> getUtxo() {
        return utxo;
    }

    public static HeadIsOpenResponse create(JsonNode raw) {
        val utxoNode = raw.get("utxo");
        val utxoMap = MoreJson.<UTXO>convertStringMap(utxoNode);

        return new HeadIsOpenResponse(utxoMap);
    }

    @Override
    public String toString() {
        return "HeadIsOpen{" +
                "utxo=" + utxo +
                ", tag=" + tag +
                '}';
    }
}

// {
//     "tag": "HeadIsOpen",
//     "utxo": {
//         "5c05106772f97eacce0a31ac215ef87aa0746279008bdb3cd07e3abece6d3985#0": {
//             "address": "addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k",
//             "datum": null,
//             "datumhash": null,
//             "inlineDatum": null,
//             "referenceScript": null,
//             "value": {
//                 "lovelace": 500000000
//             }
//         }
//     }
// }
