package org.cardanofoundation.hydra.client.model.query.response;

import lombok.Getter;
import lombok.ToString;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

import java.util.Map;

@Getter
public class HeadIsOpenResponse extends QueryResponse {

    private Map<String, UTXO> utxo;

    public HeadIsOpenResponse(Tag tag, Map<String, UTXO> utxo) {
        super(tag);
        this.utxo = utxo;
    }

    public Map<String, UTXO> getUtxo() {
        return utxo;
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
