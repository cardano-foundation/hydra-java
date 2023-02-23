package org.cardanofoundation.hydra.client.model.query.request.base;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Tag {

    Init("Init", List.of(Type.Request), List.of(Scope.Global)), // init is initialized only once by one participant

    Commit("Commit", List.of(Type.Request), List.of(Scope.Local)), // each hydra head participant commits

    Committed("Committed", List.of(Type.Response), List.of(Scope.Local, Scope.Global)),
    ReadyToCommit("ReadyToCommit", List.of(Type.Response), List.of(Scope.Global)),

    HeadIsOpen("HeadIsOpen", List.of(Type.Response), List.of(Scope.Global)),

    CommandFailed("CommandFailed", List.of(Type.Response), List.of(Scope.Local, Scope.Global)),

    PostTxOnChainFailed("PostTxOnChainFailed", List.of(Type.Response), List.of(Scope.Local)),

    PeerConnected("PeerConnected", List.of(Type.Response), List.of(Scope.Global, Scope.Local)),

    Greetings("Greetings", List.of(Type.Response), List.of(Scope.Local)),

    RolledBack("RolledBack", List.of(Type.Status), List.of(Scope.Global)),

    NewTx("NewTx", List.of(Type.Request), List.of(Scope.Local, Scope.Global));

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
