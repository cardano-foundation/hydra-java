package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.Transaction;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

@Getter
// An observed transaction is expired. The transactions is no longer valid because its ttl got to zero.
public class TxExpiredResponse extends QueryResponse {

    private final Transaction transaction;

    public TxExpiredResponse(Transaction transaction) {
        super(Tag.TxExpired);
        this.transaction = transaction;
    }

    public static TxExpiredResponse create(JsonNode raw) {
        val transaction = MoreJson.convert(raw.get("transaction"), Transaction.class);

        return new TxExpiredResponse(transaction);
    }

    @Override
    public String toString() {
        return "TxExpired{" +
                "transaction=" + transaction +
                ", tag=" + tag +
                '}';
    }
}
