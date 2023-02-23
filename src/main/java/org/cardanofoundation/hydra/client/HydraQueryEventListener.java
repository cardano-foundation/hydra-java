package org.cardanofoundation.hydra.client;

import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;

import java.util.EventListener;

public interface HydraQueryEventListener extends EventListener  {

    void onQueryResponse(QueryResponse response);

}
