package org.cardanofoundation.hydra.client.model.query.response;

import org.cardanofoundation.hydra.client.model.Tag;

// TODO all the error handling according to the docs - https://hydra.family/head-protocol/api-reference
public class PostTxOnChainFailedResponse extends Response {

    public PostTxOnChainFailedResponse() {
        super(Tag.PostTxOnChainFailed);
    }

    @Override
    public String toString() {
        return "PostTxOnChainFailedResponse{" +
                "tag=" + tag +
                '}';
    }

}
