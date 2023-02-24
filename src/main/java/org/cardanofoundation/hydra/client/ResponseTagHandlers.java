package org.cardanofoundation.hydra.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.query.response.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ResponseTagHandlers {

    private Map<Tag, Function<JsonNode, Response>> handlers = new HashMap<>();

    public ResponseTagHandlers() {
        handlers.put(Tag.Greetings, GreetingsResponse::create);
        handlers.put(Tag.PeerConnected, PeerConnectedResponse::create);
        handlers.put(Tag.PeerDisconnected, PeerDisconnectedResponse::create);
        handlers.put(Tag.ReadyToCommit, ReadyToCommitResponse::create);
        handlers.put(Tag.Committed, CommittedResponse::create);
        handlers.put(Tag.HeadIsOpen, HeadIsOpenResponse::create);
        handlers.put(Tag.HeadIsClosed, HeadIsClosedResponse::create);
        handlers.put(Tag.HeadIsContested, HeadIsContestedResponse::create);
        handlers.put(Tag.ReadyToFanout, ReadyToFanoutResponse::create);
        handlers.put(Tag.HeadIsAborted, HeadIsAbortedResponse::create);
        handlers.put(Tag.HeadIsFinalized, HeadIsFinalizedResponse::create);
        handlers.put(Tag.TxSeen, TxSeenResponse::create);
        handlers.put(Tag.TxValid, TxValidResponse::create);
        handlers.put(Tag.TxInvalid, TxInvalidResponse::create);
        handlers.put(Tag.TxExpired, TxExpiredResponse::create);
        handlers.put(Tag.GetUTxOResponse, GetUTxOResponse::create);
        handlers.put(Tag.InvalidInput, InvalidInputResponse::create);
        handlers.put(Tag.PostTxOnChainFailed, PostChainTxFailedResponse::create);
        handlers.put(Tag.RolledBack, RolledbackResponse::create);
        handlers.put(Tag.CommandFailed, CommandFailedResponse::create);
    }

    public Optional<Function<JsonNode, Response>> responseHandlerFor(Tag tag) {
        return Optional.ofNullable(handlers.get(tag));
    }

}
