package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// A `Commit` from a head participant has been observed on-chain.
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

    public Map<String, UTXO> getUtxo() {
        return utxo;
    }

    public Party getParty() {
        return party;
    }

    public int getSeq() {
        return seq;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Committed{" +
                "party=" + party +
                ", utxo=" + utxo +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
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