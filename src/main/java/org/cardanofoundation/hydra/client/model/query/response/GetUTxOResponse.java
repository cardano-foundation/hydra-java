package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// Emitted as a result of a `GetUTxO` to reflect the current UTxO of the underlying node.
public class GetUTxOResponse extends Response {

    private final String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    private final Map<String, UTXO> utxo;

    public GetUTxOResponse(String headId, int seq, LocalDateTime timestamp, Map<String, UTXO> utxo) {
        super(Tag.GetUTxOResponse);
        this.headId = headId;
        this.seq = seq;
        this.timestamp = timestamp;
        this.utxo = utxo;
    }

    public static GetUTxOResponse create(JsonNode raw) {
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));

        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new GetUTxOResponse(headId, seq, timestamp, utxo);
    }

    public String getHeadId() {
        return headId;
    }

    public int getSeq() {
        return seq;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, UTXO> getUtxo() {
        return utxo;
    }

    @Override
    public String toString() {
        return "GetUTxO{" +
                "headId='" + headId + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", utxo=" + utxo +
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
