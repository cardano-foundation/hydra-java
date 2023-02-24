package org.cardanofoundation.hydra.client.model.query.response;

import lombok.Getter;
import lombok.Setter;
import org.cardanofoundation.hydra.client.model.Tag;

@Getter
@Setter
public class Response {

    protected Tag tag;

    protected Response(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

}
