package org.cardanofoundation.hydra.client;

import org.cardanofoundation.hydra.client.model.query.response.Response;

import java.util.EventListener;

public interface HydraQueryEventListener extends EventListener  {

    void onResponse(Response response);

}
