package org.cardanofoundation.hydra.core.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.utils.MoreJson;

import java.time.LocalDateTime;

// Emitted by the server when it has failed to parse some client input. It returns the malformed input as well as some hint about what went wrong.
@Getter
@ToString(callSuper = true)
public class InvalidInputResponse extends Response implements FailureResponse {

    private final LocalDateTime timestamp;

    private final String reason;
    private final String input;

    public InvalidInputResponse(int seq, LocalDateTime timestamp, String reason, String input) {
        super(Tag.InvalidInput, seq, true);
        this.timestamp = timestamp;
        this.reason = reason;
        this.input = input;
    }

    public static InvalidInputResponse create(JsonNode raw) {
        val reason = raw.get("reason").asText();
        val input = raw.get("input").asText();

        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new InvalidInputResponse(seq, timestamp, reason, input);
    }

}
