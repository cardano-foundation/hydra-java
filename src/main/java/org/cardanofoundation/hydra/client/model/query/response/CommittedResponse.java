package org.cardanofoundation.hydra.client.model.query.response;

import lombok.Getter;
import lombok.Setter;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

import java.util.Map;

@Getter
@Setter
public class CommittedResponse extends QueryResponse {

    Party party;
    Map<String, UTXO> utxo;

    public CommittedResponse(Party party, Map<String, UTXO> utxo) {
        super(Tag.Committed);
        this.party = party;
        this.utxo = utxo;
    }

    public Map<String, UTXO> getUtxo() {
        return utxo;
    }

    public Party getParty() {
        return party;
    }

    @Override
    public String toString() {
        return "Committed{" +
                "party=" + party +
                ", utxo=" + utxo +
                ", tag=" + tag +
                '}';
    }

}

// {
//     "party": {
//         "vkey": "f68e5624f885d521d2f43c3959a0de70496d5464bd3171aba8248f50d5d72b41"
//     },
//     "tag": "Committed",
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

//{
//    "party": {
//        "vkey": "7abcda7de6d883e7570118c1ccc8ee2e911f2e628a41ab0685ffee15f39bba96"
//    },
//    "tag": "Committed",
//    "utxo": {}
//}