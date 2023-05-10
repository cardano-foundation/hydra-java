package org.cardanofoundation.hydra.client.highlevel;

import lombok.Getter;

@Getter
public class TxResult implements Response {

    private String txId;
    private boolean isValid;

    //@Nullable
    private String message;

    public TxResult(String txId, boolean isValid, String message) {
        this.txId = txId;
        this.isValid = isValid;
        this.message = message;
    }

    public TxResult(String txId, boolean isValid) {
        this.txId = txId;
        this.isValid = isValid;
    }

}
