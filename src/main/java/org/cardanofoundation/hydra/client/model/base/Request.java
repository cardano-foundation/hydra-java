package org.cardanofoundation.hydra.client.model.base;

import org.cardanofoundation.hydra.client.model.query.request.base.Tag;

public abstract class Request {

    protected Tag tag;

    protected Request(Tag tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return String.format("Request(tag:%s)", tag);
    }

    protected abstract String getMethodType();

    protected abstract String getRequestBody();

}
