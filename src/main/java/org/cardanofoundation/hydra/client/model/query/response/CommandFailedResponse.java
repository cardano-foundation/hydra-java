package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// Emitted by the server when a well-formed client input was not processable. For example, if trying to close a non opened head or, when trying to commit after having already committed.
public class CommandFailedResponse extends Response {

    private final int seq;

    private final LocalDateTime timestamp;

    private final JsonNode clientInput;

    public CommandFailedResponse(int seq,
                                 LocalDateTime timestamp,
                                 JsonNode clientInput) {
        super(Tag.CommandFailed);
        this.seq = seq;
        this.timestamp = timestamp;
        this.clientInput = clientInput;
    }

    public Map<String, ?> getClientInput() {
        return MoreJson.convertStringMap(clientInput);
    }

    public int getSeq() {
        return seq;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public static CommandFailedResponse create(JsonNode raw) {
        val clientInputNode = raw.get("clientInput");
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new CommandFailedResponse(seq, timestamp, clientInputNode);
    }

    @Override
    public String toString() {
        return "CommandFailed{" +
                "seq=" + seq +
                ", timestamp=" + timestamp +
                ", clientInput=" + clientInput +
                ", tag=" + tag +
                '}';
    }
}
