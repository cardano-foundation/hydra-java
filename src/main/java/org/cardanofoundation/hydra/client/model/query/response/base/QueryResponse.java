package org.cardanofoundation.hydra.client.model.query.response.base;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;

@Getter
@Setter
public class QueryResponse {

    protected Tag tag;

    protected QueryResponse(Tag tag) {
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

}
