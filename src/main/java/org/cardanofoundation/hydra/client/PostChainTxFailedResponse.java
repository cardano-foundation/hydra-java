package org.cardanofoundation.hydra.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

public class PostChainTxFailedResponse extends QueryResponse {

    public PostChainTxFailedResponse() {
        super(Tag.PostTxOnChainFailed);
    }

    public static PostChainTxFailedResponse create(JsonNode raw) {
        return new PostChainTxFailedResponse();
    }

    @Override
    public String toString() {
        return "PostChainTxFailed{" +
                "tag=" + tag +
                '}';
    }

}
