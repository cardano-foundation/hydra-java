package org.cardanofoundation.hydra.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.model.query.response.*;
import org.cardanofoundation.hydra.core.store.UTxOStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ResponseTagHandlers {

    private final Map<Tag, Function<JsonNode, Response>> handlers = new HashMap<>();

    public ResponseTagHandlers(UTxOStore uTxOStore) {
        handlers.put(Tag.Greetings, raw -> GreetingsResponse.create(uTxOStore, raw));
        handlers.put(Tag.PeerConnected, PeerConnectedResponse::create);
        handlers.put(Tag.PeerDisconnected, PeerDisconnectedResponse::create);
        handlers.put(Tag.HeadIsInitializing, HeadIsInitializingResponse::create);
        handlers.put(Tag.Committed, CommittedResponse::create);
        handlers.put(Tag.HeadIsOpen, raw -> HeadIsOpenResponse.create(uTxOStore, raw));
        handlers.put(Tag.HeadIsClosed, HeadIsClosedResponse::create);
        handlers.put(Tag.HeadIsContested, HeadIsContestedResponse::create);
        handlers.put(Tag.ReadyToFanout, ReadyToFanoutResponse::create);
        handlers.put(Tag.HeadIsAborted, HeadIsAbortedResponse::create);
        handlers.put(Tag.HeadIsFinalized, HeadIsFinalizedResponse::create);
        handlers.put(Tag.TxValid, TxValidResponse::create);
        handlers.put(Tag.TxInvalid, TxInvalidResponse::create);
        handlers.put(Tag.GetUTxOResponse, GetUTxOResponse::create);
        handlers.put(Tag.InvalidInput, InvalidInputResponse::create);
        handlers.put(Tag.PostTxOnChainFailed, PostTxOnChainFailedResponse::create);
        handlers.put(Tag.RolledBack, RolledbackResponse::create);
        handlers.put(Tag.CommandFailed, CommandFailedResponse::create);
        handlers.put(Tag.SnapshotConfirmed, raw -> SnapshotConfirmed.create(uTxOStore, raw));
    }

    public Optional<Function<JsonNode, Response>> responseHandlerFor(Tag tag) {
        return Optional.ofNullable(handlers.get(tag));
    }

}
