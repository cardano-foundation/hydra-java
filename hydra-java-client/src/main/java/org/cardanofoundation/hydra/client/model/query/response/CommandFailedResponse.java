package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.utils.MoreJson;
import org.cardanofoundation.hydra.client.model.Tag;

import java.time.LocalDateTime;

// Emitted by the server when a well-formed client input was not processable. For example, if trying to close a non opened head or, when trying to commit after having already committed.
@Getter
@ToString(callSuper = true)
public class CommandFailedResponse extends Response implements FailureResponse {

    private final int seq;

    private final LocalDateTime timestamp;

    private final JsonNode clientInput;

    public CommandFailedResponse(int seq,
                                 LocalDateTime timestamp,
                                 JsonNode clientInput) {
        super(Tag.CommandFailed, seq, true);
        this.seq = seq;
        this.timestamp = timestamp;
        this.clientInput = clientInput;
    }

    public static CommandFailedResponse create(JsonNode raw) {
        val clientInputNode = raw.get("clientInput");
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new CommandFailedResponse(seq, timestamp, clientInputNode);
    }

}
