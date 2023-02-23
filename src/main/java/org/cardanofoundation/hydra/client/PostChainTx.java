package org.cardanofoundation.hydra.client;

import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

public class PostChainTx extends QueryResponse {

    public PostChainTx() {
        super(Tag.PostTxOnChainFailed);
    }

}
