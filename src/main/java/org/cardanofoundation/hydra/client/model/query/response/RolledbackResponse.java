package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import org.cardanofoundation.hydra.client.model.Tag;

// The node has adopted a different chain fork and we had to rollback the application state.
public class RolledbackResponse extends Response {

    public RolledbackResponse() {
        super(Tag.RolledBack);
    }

    public static RolledbackResponse create(JsonNode raw) {
        return new RolledbackResponse();
    }

    @Override
    public String toString() {
        return "Rolledback{" + "tag=" + tag + '}';
    }

}
