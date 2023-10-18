package org.cardanofoundation.hydra.core.model.query.response;

import lombok.Getter;
import org.cardanofoundation.hydra.core.model.Tag;

@Getter
public class Response {

    protected final Tag tag;
    protected final int seq;
    protected final boolean isFailure;

    protected Response(Tag tag,
                       int seq,
                       boolean isFailure) {
        this.tag = tag;
        this.seq = seq;
        this.isFailure = isFailure;
    }

    protected Response(Tag tag, int seq) {
        this.tag = tag;
        this.seq = seq;
        this.isFailure = false;
    }

    @Override
    public String toString() {
        return String.format("{tag:%s, seq:%s}", tag.name(), seq);
    }

}
