package org.cardanofoundation.hydra.core.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.model.Transaction;
import org.cardanofoundation.hydra.core.utils.MoreJson;

import java.time.LocalDateTime;

// An observed transaction is valid and can therefore be applied.
@Getter
@ToString(callSuper = true)
public class TxValidResponse extends Response {

    private final Transaction transaction;

    private final String headId;

    private final LocalDateTime timestamp;

    public TxValidResponse(Transaction transaction, String headId, int seq, LocalDateTime timestamp) {
        super(Tag.TxValid, seq);
        this.transaction = transaction;
        this.headId = headId;
        this.timestamp = timestamp;
    }

    public static TxValidResponse create(JsonNode raw) {
        val transaction = MoreJson.convert(raw.get("transaction"), Transaction.class);
        val headId = raw.get("headId").asText();
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new TxValidResponse(transaction, headId, seq, timestamp);
    }

}
