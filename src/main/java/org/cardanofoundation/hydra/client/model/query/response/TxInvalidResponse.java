package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.Transaction;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.ValidationError;
import org.cardanofoundation.hydra.client.internal.utils.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// An observed transaction is invalid. Either it is not yet valid (because some other transactions need to be seen first), or it
// is no longer valid (because of conflicting transactions observed in-between.
@Getter
@ToString(callSuper = true)
public class TxInvalidResponse extends Response implements FailureResponse {

    private final String headId;

    private final LocalDateTime timestamp;

    private final Map<String, UTXO> utxo;
    private final Transaction transaction;
    private final ValidationError validationError;

    public TxInvalidResponse(String headId,
                             int seq,
                             LocalDateTime timestamp,
                             Map<String, UTXO> utxo,
                             Transaction transaction,
                             ValidationError validationError) {
        super(Tag.TxInvalid, seq, true);
        this.headId = headId;
        this.timestamp = timestamp;
        this.utxo = utxo;
        this.transaction = transaction;
        this.validationError = validationError;
    }

    public static TxInvalidResponse create(JsonNode raw) {
        val utxo = MoreJson.convertUTxOMap(raw.get("utxo"));
        val transaction = MoreJson.convert(raw.get("transaction"), Transaction.class);
        val validationError = MoreJson.convert(raw.get("validationError"), ValidationError.class);
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new TxInvalidResponse(headId, seq, timestamp, utxo, transaction, validationError);
    }

}
