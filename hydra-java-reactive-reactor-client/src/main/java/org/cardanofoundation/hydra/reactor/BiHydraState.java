package org.cardanofoundation.hydra.reactor;

import org.cardanofoundation.hydra.core.model.HydraState;

/**
 * Represents a pair of Hydra states, typically representing the transition from an old state to a new state.
 * This class is annotated with Lombok's annotations for generating constructors, getters, and toString methods.
 */
public record BiHydraState(HydraState oldState, HydraState newState) {
}
