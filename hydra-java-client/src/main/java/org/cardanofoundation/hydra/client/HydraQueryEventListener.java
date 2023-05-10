package org.cardanofoundation.hydra.client;

import org.cardanofoundation.hydra.client.model.query.response.Response;

import java.util.EventListener;

public interface HydraQueryEventListener extends EventListener  {

    void onResponse(Response response);

    void onSuccess(Response response);

    void onFailure(Response response);

    class Stub implements HydraQueryEventListener {

        @Override
        public void onResponse(Response response) {}

        @Override
        public void onSuccess(Response response) {}

        @Override
        public void onFailure(Response response) {}
    }

}
