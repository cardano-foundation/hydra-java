package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Map;

// Emitted by the server when a well-formed client input was not processable. For example, if trying to close a non opened head or, when trying to commit after having already committed.
public class CommandFailedResponse extends Response {

    private final JsonNode clientInput;

    public CommandFailedResponse(JsonNode clientInput) {
        super(Tag.CommandFailed);
        this.clientInput = clientInput;
    }

    public Map<String, ?> getClientInput() {
        return MoreJson.convertStringMap(clientInput);
    }

    public static CommandFailedResponse create(JsonNode raw) {
        val clientInputNode = raw.get("clientInput");

        return new CommandFailedResponse(clientInputNode);
    }

    @Override
    public String toString() {
        return "CommandFailed{" +
                "clientInput=" + getClientInput() +
                ", tag=" + tag +
                '}';
    }

}
