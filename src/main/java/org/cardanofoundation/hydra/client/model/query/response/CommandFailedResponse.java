package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Map;

public class CommandFailedResponse extends QueryResponse {

    private JsonNode clientInput;

    public CommandFailedResponse(JsonNode clientInput) {
        super(Tag.CommandFailed);
        this.clientInput = clientInput;
    }

    public Map<String, ?> getClientInput() {
        return MoreJson.convertStringMap(clientInput);
    }

    @Override
    public String toString() {
        return "CommandFailed{" +
                "clientInput=" + getClientInput() +
                ", tag=" + tag +
                '}';
    }

}
