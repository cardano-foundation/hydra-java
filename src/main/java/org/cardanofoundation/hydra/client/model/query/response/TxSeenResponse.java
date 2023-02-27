package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Transaction;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

@Getter
// A new transaction was observed inside the head. Note that a node observes its own transactions.
public class TxSeenResponse extends Response {

    private final Transaction transaction;

    public TxSeenResponse(Transaction transaction) {
        super(Tag.TxSeen);
        this.transaction = transaction;
    }

    public static TxSeenResponse create(JsonNode raw) {
        val transaction = MoreJson.convert(raw.get("transaction"), Transaction.class);

        return new TxSeenResponse(transaction);
    }

    @Override
    public String toString() {
        return "TxSeen{" +
                "transaction=" + transaction +
                ", tag=" + tag +
                '}';
    }
}

// {
//     "tag": "TxSeen",
//     "transaction": {
//         "body": {
//             "fees": 0,
//             "inputs": [
//                 "0f5d9bc80894a3938d67d78336ecfa437d7272de52bb3303556cefd282fe1e20#0"
//             ],
//             "outputs": [
//                 {
//                     "address": "addr_test1vqneq3v0dqh3x3muv6ee3lt8e5729xymnxuavx6tndcjc2cv24ef9",
//                     "datum": null,
//                     "datumhash": null,
//                     "inlineDatum": null,
//                     "referenceScript": null,
//                     "value": {
//                         "lovelace": 1000000000
//                     }
//                 },
//                 {
//                     "address": "addr_test1vrwnl84mn56q6ffx06qu58kvxpk399fal627h37lfjwy40cxykgkv",
//                     "datum": null,
//                     "datumhash": null,
//                     "inlineDatum": null,
//                     "referenceScript": null,
//                     "value": {
//                         "lovelace": 3899832651
//                     }
//                 }
//             ]
//         },
//         "id": "28deef61b098c4608bfc9913dbe1488072c9c289d4c0bdf165db58320439ebf9",
//         "isValid": true,
//         "witnesses": {
//             "keys": [
//                 "82008258205ef70cf2ef40cec074a3835daa95c133a00faca8a70143a837e28585203db6815840b584465704a250f515c86efbcf3705c7deae82132de62103173f3ab7fe838a59e071025e2e6c57150f575bd838acbb76323cea197f23f2aab827c07507705905"
//             ]
//         }
//        }
// }
