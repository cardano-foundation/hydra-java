package org.cardanofoundation.hydra.client.model.query.response;

import org.cardanofoundation.hydra.client.model.Tag;

public class Response {

    protected final Tag tag;

    protected Response(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return tag.name();
    }

}
