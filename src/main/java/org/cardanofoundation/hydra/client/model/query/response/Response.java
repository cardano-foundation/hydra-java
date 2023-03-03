package org.cardanofoundation.hydra.client.model.query.response;

import lombok.Getter;
import org.cardanofoundation.hydra.client.model.Tag;

@Getter
public class Response {

    protected final Tag tag;

    protected Response(Tag tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return tag.name();
    }

}
