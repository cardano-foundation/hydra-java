package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;
import java.util.List;

// A `Init` transaction has been observed on-chain by the given party who's now ready to commit into the initialized head.
public class HeadIsInitilizingResponse extends Response {

    private final List<Party> parties;

    private String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    public HeadIsInitilizingResponse(String headId, List<Party> parties, int seq, LocalDateTime timestamp) {
        super(Tag.HeadIsInitilizing);
        this.headId = headId;
        this.parties = parties;
        this.seq = seq;
        this.timestamp = timestamp;
    }

    public List<Party> getParties() {
        return parties;
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

    public static HeadIsInitilizingResponse create(JsonNode raw) {
        val utxoNode = raw.get("parties");
        val parties = MoreJson.<Party>convertList(utxoNode);
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new HeadIsInitilizingResponse(headId, parties, seq, timestamp);
    }

    @Override
    public String toString() {
        return "HeadIsInitilizing{" +
                "parties=" + parties +
                ", headId='" + headId + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", tag=" + tag +
                '}';
    }

}

// {
//    "parties": [
//        {
//            "vkey": "7abcda7de6d883e7570118c1ccc8ee2e911f2e628a41ab0685ffee15f39bba96"
//        },
//        {
//            "vkey": "b37aabd81024c043f53a069c91e51a5b52e4ea399ae17ee1fe3cb9c44db707eb"
//        },
//        {
//            "vkey": "f68e5624f885d521d2f43c3959a0de70496d5464bd3171aba8248f50d5d72b41"
//        }
//    ],
//    "tag": "ReadyToCommit"
//}