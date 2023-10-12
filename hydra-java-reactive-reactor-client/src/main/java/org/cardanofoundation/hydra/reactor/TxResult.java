package org.cardanofoundation.hydra.reactor;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class TxResult {

    private final String txId;
    private final boolean isValid;

    @Nullable private String message;

    public TxResult(String txId,
                    boolean isValid,
                    String message) {
        this.txId = txId;
        this.isValid = isValid;
        this.message = message;
    }

    public TxResult(String txId, boolean isValid) {
        this.txId = txId;
        this.isValid = isValid;
    }

}
