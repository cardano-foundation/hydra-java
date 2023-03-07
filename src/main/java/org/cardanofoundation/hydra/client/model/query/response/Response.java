package org.cardanofoundation.hydra.client.model.query.response;

import lombok.Getter;
import org.cardanofoundation.hydra.client.model.Tag;

@Getter
public class Response {

    protected final Tag tag;
    protected final int seq;

    protected Response(Tag tag, int seq) {
        this.tag = tag;
        this.seq = seq;
    }

    @Override
    public String toString() {
        return String.format("{tag:%s, seq:%s}", tag.name(), seq);
    }

}
