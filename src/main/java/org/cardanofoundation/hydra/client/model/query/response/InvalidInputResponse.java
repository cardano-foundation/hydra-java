package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

@Getter
// Emitted by the server when it has failed to parse some client input. It returns the malformed input as well as some hint about what went wrong.
public class InvalidInputResponse extends QueryResponse {

    private final String reason;
    private final String input;

    public InvalidInputResponse(String reason, String input) {
        super(Tag.InvalidInput);
        this.reason = reason;
        this.input = input;
    }

    public static InvalidInputResponse create(JsonNode raw) {
        val reason = raw.get("reason").asText();
        val input = raw.get("input").asText();

        return new InvalidInputResponse(reason, input);
    }

    @Override
    public String toString() {
        return "InvalidInputResponse{" +
                "reason='" + reason + '\'' +
                ", input='" + input + '\'' +
                ", tag=" + tag +
                '}';
    }

}
