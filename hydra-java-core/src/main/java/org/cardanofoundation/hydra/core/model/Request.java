package org.cardanofoundation.hydra.core.model;

public abstract class Request {

    protected Tag tag;

    protected Request(Tag tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return String.format("Request(tag:%s)", tag);
    }

    protected abstract String getRequestBody();

}
