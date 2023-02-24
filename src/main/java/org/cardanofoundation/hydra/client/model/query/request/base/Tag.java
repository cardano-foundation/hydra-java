package org.cardanofoundation.hydra.client.model.query.request.base;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Tag {

    Init("Init", List.of(Type.Request), List.of(Scope.Global)), // init is initialized only once by one participant

    Commit("Commit", List.of(Type.Request), List.of(Scope.Local, Scope.Global)), // each hydra head participant commits

    Committed("Committed", List.of(Type.Response), List.of(Scope.Local, Scope.Global)),

    ReadyToCommit("ReadyToCommit", List.of(Type.Status, Type.Response), List.of(Scope.Global)),

    // After each party has made the commit action, the hydra-node will automatically send the transaction to the mainchain that will collect all these funds and will open the head. This is logged by the Web Socket as
    HeadIsOpen("HeadIsOpen", List.of(Type.Status), List.of(Scope.Global)),

    HeadIsClosed("HeadIsClosed", List.of(Type.Status, Type.Response), List.of(Scope.Global)),

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

    TxValid("TxValid", List.of(Type.Response), List.of(Scope.Local)),

    TxSeen("TxSeen", List.of(Type.Status), List.of(Scope.Global)), // status message that some other hydra head operator broadcast transaction

    Close("Close", List.of(Type.Request), List.of(Scope.Global));

    private final String value;
    private final List<Type> types;
    private final List<Scope> scopes;

    Tag(String value,
        List<Type> types,
        List<Scope> scopes) {
        this.value = value;
        this.types = types;
        this.scopes = scopes;
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
