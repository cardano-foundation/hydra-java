package org.cardanofoundation.hydra.client.model.query.response;

import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

public class Greetings extends QueryResponse {

    private final Party me;

    public Greetings(Party party) {
        super(Tag.Greetings);
        this.me = party;
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
