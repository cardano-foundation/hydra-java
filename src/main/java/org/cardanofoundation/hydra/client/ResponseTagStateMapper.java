package org.cardanofoundation.hydra.client;

import org.cardanofoundation.hydra.client.model.HydraState;
import org.cardanofoundation.hydra.client.model.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// https://hydra.family/head-protocol/core-concepts/behavior/
public class ResponseTagStateMapper {

    private Map<Tag, HydraState> tagToStateMap = new HashMap<>();

    public ResponseTagStateMapper() {
        tagToStateMap.put(Tag.ReadyToCommit, HydraState.Initializing);
        tagToStateMap.put(Tag.HeadIsOpen, HydraState.Open);
        tagToStateMap.put(Tag.HeadIsClosed, HydraState.Closed);
        tagToStateMap.put(Tag.HeadIsAborted, HydraState.Final);
        tagToStateMap.put(Tag.HeadIsFinalized, HydraState.Final);
        tagToStateMap.put(Tag.HeadIsContested, HydraState.Final);
        tagToStateMap.put(Tag.ReadyToFanout, HydraState.FanoutPossible);
        tagToStateMap.put(Tag.ReadyToFanout, HydraState.FanoutPossible);
    }

    public Optional<HydraState> stateForTag(Tag tag) {
        return Optional.ofNullable(tagToStateMap.get(tag));
    }

}
