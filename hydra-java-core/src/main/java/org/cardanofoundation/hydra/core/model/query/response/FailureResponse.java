package org.cardanofoundation.hydra.core.model.query.response;

public interface FailureResponse {

    default boolean isLowLevelFailure() {
        return false;
    }

}
