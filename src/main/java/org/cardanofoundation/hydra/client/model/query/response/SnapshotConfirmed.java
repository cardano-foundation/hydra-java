package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Snapshot;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

// A `Init` transaction has been observed on-chain by the given party who's now ready to commit into the initialized head.
public class SnapshotConfirmed extends Response {

    private final String headId;

    private final int seq;

    private final LocalDateTime timestamp;

    private final Snapshot snapshot;

    public SnapshotConfirmed(String headId, int seq, LocalDateTime timestamp, Snapshot snapshot) {
        super(Tag.SnapshotConfirmed);
        this.headId = headId;
        this.seq = seq;
        this.timestamp = timestamp;
        this.snapshot = snapshot;
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

    public Snapshot getSnapshot() {
        return snapshot;
    }

    public static SnapshotConfirmed create(JsonNode raw) {
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);
        val snapshot = MoreJson.convert(raw.get("snapshot"), Snapshot.class);

        return new SnapshotConfirmed(headId, seq, timestamp, snapshot);
    }

    @Override
    public String toString() {
        return "SnapshotConfirmed{" +
                "headId='" + headId + '\'' +
                ", seq=" + seq +
                ", timestamp=" + timestamp +
                ", snapshot=" + snapshot +
                ", tag=" + tag +
                '}';
    }
}

// {
//   "tag": "SnapshotConfirmed",
//   "headId": "820082582089ff4f3ff4a6052ec9d073",
//   "snapshot": {
//     "snapshotNumber": 0,
//     "utxo": {
//       "09d34606abdcd0b10ebc89307cbfa0b469f9144194137b45b7a04b273961add8#687": {
//         "address": "addr1w9htvds89a78ex2uls5y969ttry9s3k9etww0staxzndwlgmzuul5",
//         "value": {
//           "lovelace": 7620669
//         }
//       }
//     },
//     "confirmedTransactions": [
//       {
//         "id": "7ca4e30387ec4ba0e95604fdab6e867fc3d740220386e1a63d142c71e8eac4ce",
//         "isValid": true,
//         "auxiliaryData": "d90103a100a30181a40401622c7166f098a89d2a7e006023600a600c66e4a99d7f6b08",
//         "body": {
//           "inputs": [
//             "03170a2e7597b7b7e3d84c05391d139a62b157e78786d8c082f29dcf4c111314#116",
//             "2208e439244a1d0ef238352e3693098aba9de9dd0154f9056551636c8ed15dc1#149"
//           ],
//           "outputs": [
//             {
//               "address": "addr1w9htvds89a78ex2uls5y969ttry9s3k9etww0staxzndwlgmzuul5",
//               "datumhash": "2208e439244a1d0ef238352e3693098aba9de9dd0154f9056551636c8ed15dc1",
//               "value": {
//                 "lovelace": 12,
//                 "4acf2773917c7b547c576a7ff110d2ba5733c1f1ca9cdc659aea3a56": {
//                   "91c670": 7
//                 }
//               }
//             }
//           ],
//           "fees": 0
//         },
//         "witnesses": {
//           "redeemers": "84840000...e83fccf5",
//           "keys": [
//             "82008258...01ad847b",
//             "82008258...7944fe3e"
//           ],
//           "scripts": {
//             "1be26e9d1710022443c8043b259f7b375ec8732191f3845a6aea28e5": "8200820181820519c355"
//           },
//           "datums": {
//             "ae85d245a3d00bfde01f59f3c4fe0b4bfae1cb37e9cf91929eadcea4985711de": "20"
//           }
//         }
//       }
//     ]
//   },
//   "seq": 1,
//   "timestamp": "2019-08-24T14:15:22Z"
// }