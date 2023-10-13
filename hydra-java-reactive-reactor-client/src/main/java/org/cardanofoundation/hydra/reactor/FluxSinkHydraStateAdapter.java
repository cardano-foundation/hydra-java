package org.cardanofoundation.hydra.reactor;

import org.cardanofoundation.hydra.client.HydraStateEventListener;
import org.cardanofoundation.hydra.core.model.HydraState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nullable;

public class FluxSinkHydraStateAdapter implements HydraStateEventListener {

    @Nullable private FluxSink<BiHydraState> sink;

    public void setSink(@Nullable FluxSink<BiHydraState> sink) {
        this.sink = sink;
    }

    @Override
    public void onStateChanged(HydraState oldState, HydraState newState) {
        if (sink != null) {
            sink.next(new BiHydraState(oldState, newState));
        }
    }

}
