package org.cardanofoundation.hydra.client.model.query.request.base;

import org.cardanofoundation.hydra.client.model.HydraState;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Tag {

    Init("Init", List.of(Type.Request), List.of(Scope.Global)), // init is initialized only once by one participant

    Commit("Commit", List.of(Type.Request), List.of(Scope.Local, Scope.Global)), // each hydra head participant commits

    Committed("Committed", List.of(Type.Response), List.of(Scope.Local, Scope.Global)),

    ReadyToCommit("ReadyToCommit", List.of(Type.Status, Type.Response), List.of(Scope.Global), HydraState.Initializing),

    // After each party has made the commit action, the hydra-node will automatically send the transaction to the mainchain that will collect all these funds and will open the head. This is logged by the Web Socket as
    HeadIsOpen("HeadIsOpen", List.of(Type.Status), List.of(Scope.Global), HydraState.Open),

    HeadIsClosed("HeadIsClosed", List.of(Type.Status, Type.Response), List.of(Scope.Global), HydraState.Closed),

    HeadIsAborted("HeadIsAborted", List.of(Type.Status, Type.Response), List.of(Scope.Global), HydraState.Final),

    HeadIsFinalized("HeadIsFinalized", List.of(Type.Status, Type.Response), List.of(Scope.Global), HydraState.Final),

    HeadIsContested("HeadIsContested", List.of(Type.Status, Type.Response), List.of(Scope.Global), HydraState.Final),

    InvalidInput("InvalidInput", List.of(Type.Response), List.of(Scope.Local)),

    ReadyToFanout("ReadyToFanout", List.of(Type.Status, Type.Response), List.of(Scope.Global), HydraState.FanoutPossible),

    GetUTxO("GetUTxO", List.of(Type.Request), List.of(Scope.Local, Scope.Global)),

    GetUTxOResponse("GetUTxOResponse", List.of(Type.Response), List.of(Scope.Local, Scope.Global)),

    CommandFailed("CommandFailed", List.of(Type.Response), List.of(Scope.Local, Scope.Global)),

    PostTxOnChainFailed("PostTxOnChainFailed", List.of(Type.Response), List.of(Scope.Local)),

    PeerConnected("PeerConnected", List.of(Type.Status), List.of(Scope.Global, Scope.Local)),

    PeerDisconnected("PeerDisconnected", List.of(Type.Status), List.of(Scope.Global, Scope.Local)),

    Greetings("Greetings", List.of(Type.Response), List.of(Scope.Local)), // this is send only to a web socket client which connected

    RolledBack("RolledBack", List.of(Type.Status), List.of(Scope.Global)),

    NewTx("NewTx", List.of(Type.Request), List.of(Scope.Local, Scope.Global)),

    Abort("Abort", List.of(Type.Request), List.of(Scope.Local, Scope.Global)),

    TxValid("TxValid", List.of(Type.Response), List.of(Scope.Global)),

    TxExpired("TxExpired", List.of(Type.Response), List.of(Scope.Global)),

    TxInvalid("TxInvalid", List.of(Type.Response), List.of(Scope.Global)),

    TxSeen("TxSeen", List.of(Type.Status), List.of(Scope.Local)), // status message that some other hydra head operator broadcast transaction

    Close("Close", List.of(Type.Request), List.of(Scope.Global)),

    Contest("Contest", List.of(Type.Request), List.of(Scope.Global)),

    Fanout("Fanout", List.of(Type.Request), List.of(Scope.Global));

    private final String value;
    private final List<Type> types;
    private final List<Scope> scopes;

    private final Optional<HydraState> state;

    Tag(String value,
        List<Type> types,
        List<Scope> scopes) {
        this.value = value;
        this.types = types;
        this.scopes = scopes;
        this.state = Optional.empty();
    }

    Tag(String value,
        List<Type> types,
        List<Scope> scopes,
        HydraState state) {
        this.value = value;
        this.types = types;
        this.scopes = scopes;
        this.state = Optional.of(state);
    }

    public static Optional<Tag> find(String tag) {
        return Arrays.stream(values()).filter(qt -> qt.value.equals(tag)).findAny();
    }

    public String getValue() {
        return value;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public List<Type> getTypes() {
        return types;
    }

    public Optional<HydraState> getState() {
        return state;
    }

    enum Type {
        Request,
        Response,
        Status
    }

    enum Scope {
        Global, // mutates global state
        Local // mutates local state
    }

}
