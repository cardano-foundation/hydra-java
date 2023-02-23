package org.cardanofoundation.hydra.client.model.query.response;

import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

public class Rolledback extends QueryResponse {

    public Rolledback() {
        super(Tag.RolledBack);
    }

    @Override
    public String toString() {
        return "Rolledback{" + "tag=" + tag + '}';
    }
}
