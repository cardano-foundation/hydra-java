package org.cardanofoundation.hydra.reactor;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class TxResult {

    private final String txId;
    private final boolean isValid;

    @Nullable private String reason;

    public TxResult(String txId,
                    boolean isValid,
                    @Nullable String reason) {
        this.txId = txId;
        this.isValid = isValid;
        this.reason = reason;
    }

    public TxResult(String txId, boolean isValid) {
        this.txId = txId;
        this.isValid = isValid;
    }

}
