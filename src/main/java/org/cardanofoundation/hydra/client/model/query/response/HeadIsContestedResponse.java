package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;

@Getter
//        A `Contest` transaction has been observed on-chain, that means the head
//        state has been successfully contested and given snapshot number is now
//        the latest accepted snapshot.
public class HeadIsContestedResponse extends Response {

    private final int snapshotNumber;

    public HeadIsContestedResponse(int snapshotNumber) {
        super(Tag.HeadIsClosed);
        this.snapshotNumber = snapshotNumber;
    }

    public static HeadIsContestedResponse create(JsonNode raw) {
        val snapshotNumber = raw.get("snapshotNumber").asInt();

        return new HeadIsContestedResponse(snapshotNumber);
    }

    @Override
    public String toString() {
        return "HeadIsContested{" +
                "snapshotNumber=" + snapshotNumber +
                ", tag=" + tag +
                '}';
    }

}

//{
//        "snapshotNumber": 1,
//        "tag": "HeadIsContested"
//}