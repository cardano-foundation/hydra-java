package org.cardanofoundation.hydra.reactor;

import org.cardanofoundation.hydra.client.HydraQueryEventListener;
import org.cardanofoundation.hydra.core.model.query.response.Response;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nullable;

public class FluxSinkResponseAdapter extends HydraQueryEventListener.Stub {

    @Nullable
    private FluxSink<Response> sink;

    public void setSink(@Nullable FluxSink<Response> sink) {
        this.sink = sink;
    }

    @Override
    public void onSuccess(Response response) {
        if (sink != null) {
            sink.next(response);
        }
    }

}
