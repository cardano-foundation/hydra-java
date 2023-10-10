package org.cardanofoundation.hydra.reactor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.cardanofoundation.hydra.core.model.HydraState;

@AllArgsConstructor
@Getter
@ToString
public class BiHydraState {

    private HydraState oldState;
    private HydraState newState;

}
