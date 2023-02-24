package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

public class GreetingsResponse extends QueryResponse {

    private final Party me;

    public GreetingsResponse(Party party) {
        super(Tag.Greetings);
        this.me = party;
    }

    public static GreetingsResponse create(JsonNode raw) {
        val party = MoreJson.convert(raw.get("me"), Party.class);

        return new GreetingsResponse(party);
    }

    public Party getMe() {
        return me;
    }

    @Override
    public String toString() {
        return "Greetings{" +
                "me='" + me + '\'' +
                ", tag=" + tag +
                '}';
    }

}
