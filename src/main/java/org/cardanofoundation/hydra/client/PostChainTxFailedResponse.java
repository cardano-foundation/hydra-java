package org.cardanofoundation.hydra.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

// Something wrong happened when trying to post a transaction on-chain. Provides information about what kind of transaction was tentatively posted, and the reason for failure.
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
