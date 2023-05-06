package org.cardanofoundation.hydra.client.model.query.response;

public interface FailureResponse {

    default boolean isLowLevelFailure() {
        return false;
    }

}
