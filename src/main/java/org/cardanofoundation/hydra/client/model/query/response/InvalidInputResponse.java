package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

// Emitted by the server when it has failed to parse some client input. It returns the malformed input as well as some hint about what went wrong.
public class InvalidInputResponse extends Response {

    private final int seq;

    private final LocalDateTime timestamp;

    private final String reason;
    private final String input;

    public InvalidInputResponse(int seq, LocalDateTime timestamp, String reason, String input) {
        super(Tag.InvalidInput);
        this.seq = seq;
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

    public String getReason() {
        return reason;
    }

    public String getInput() {
        return input;
    }

    public int getSeq() {
        return seq;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "InvalidInput{" +
                "seq=" + seq +
                ", timestamp=" + timestamp +
                ", reason='" + reason + '\'' +
                ", input='" + input + '\'' +
                ", tag=" + tag +
                '}';
    }

}
