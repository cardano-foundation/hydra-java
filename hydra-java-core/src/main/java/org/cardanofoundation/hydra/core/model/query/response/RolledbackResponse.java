package org.cardanofoundation.hydra.core.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.utils.MoreJson;

import java.time.LocalDateTime;

// The node has adopted a different chain fork and we had to rollback the application state.
@Getter
@ToString(callSuper = true)
public class RolledbackResponse extends Response {

    private final LocalDateTime timestamp;

    public RolledbackResponse(int seq, LocalDateTime timestamp) {
        super(Tag.RolledBack, seq);
        this.timestamp = timestamp;
    }

    public static RolledbackResponse create(JsonNode raw) {
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new RolledbackResponse(seq, timestamp);
    }

}
