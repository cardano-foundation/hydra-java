package org.cardanofoundation.hydra.client.model;

import java.util.Arrays;
import java.util.Optional;

public enum Tag {

    Init,
    Commit,
    Committed,
    HeadIsInitializing,
    HeadIsOpen,
    HeadIsClosed,
    HeadIsAborted,
    HeadIsFinalized,
    HeadIsContested,
    InvalidInput,
    ReadyToFanout,
    GetUTxO,
    GetUTxOResponse,
    CommandFailed,
    PostTxOnChainFailed,
    PeerConnected,
    PeerDisconnected,
    Greetings,
    RolledBack,
    NewTx,
    Abort,
    TxValid,
    TxExpired,
    TxInvalid,
    TxSeen,
    Close,
    Contest,
    Fanout,
    SnapshotConfirmed;

    public static Optional<Tag> find(String tag) {
        return Arrays.stream(values()).filter(qt -> qt.name().equals(tag)).findAny();
    }

}
