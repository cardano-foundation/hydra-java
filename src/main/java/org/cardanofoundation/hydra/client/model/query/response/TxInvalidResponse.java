package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.Transaction;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.ValidationError;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Map;

@Getter
// An observed transaction is invalid. Either it is not yet valid (because some other transactions need to be seen first), or it
// is no longer valid (because of conflicting transactions observed in-between.
public class TxInvalidResponse extends QueryResponse {

    private final Map<String, UTXO> utxo;
    private final Transaction transaction;
    private final ValidationError validationError;

    public TxInvalidResponse(Map<String, UTXO> utxo,
                             Transaction transaction,
                             ValidationError validationError) {
        super(Tag.TxInvalid);
        this.utxo = utxo;
        this.transaction = transaction;
        this.validationError = validationError;
    }

    public static TxInvalidResponse create(JsonNode raw) {
        val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));
        val transaction = MoreJson.convert(raw.get("transaction"), Transaction.class);
        val validationError = MoreJson.convert(raw.get("validationError"), ValidationError.class);

        return new TxInvalidResponse(utxo, transaction, validationError);
    }

    @Override
    public String toString() {
        return "TxInvalid{" +
                "utxo=" + utxo +
                ", transaction=" + transaction +
                ", validationError=" + validationError +
                ", tag=" + tag +
                '}';
    }
}
