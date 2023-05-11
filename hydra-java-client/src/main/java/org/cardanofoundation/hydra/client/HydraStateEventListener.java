package org.cardanofoundation.hydra.client;

import org.cardanofoundation.hydra.core.model.HydraState;

import java.util.EventListener;

public interface HydraStateEventListener extends EventListener {

    void onStateChanged(HydraState prevState, HydraState newState);

}
