package org.cardanofoundation.hydra.reactor;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a transaction submitted to the Hydra network.
 */
@Getter
public class TxResult {

    /**
     * The unique identifier of the transaction.
     */
    private final String txId;

    /**
     * Indicates whether the transaction is valid or not.
     */
    private final boolean isValid;

    /**
     * An optional reason explaining why the transaction is invalid.
     */
    @Nullable private String reason;

    /**
     * Constructs a new instance of TxResult with the specified parameters.
     *
     * @param txId   The unique identifier of the transaction.
     * @param isValid Indicates whether the transaction is valid or not.
     * @param reason An optional reason explaining why the transaction is invalid.
     */
    public TxResult(String txId, boolean isValid, @Nullable String reason) {
        this.txId = txId;
        this.isValid = isValid;
        this.reason = reason;
    }

    /**
     * Constructs a new instance of TxResult with the specified parameters.
     *
     * @param txId   The unique identifier of the transaction.
     * @param isValid Indicates whether the transaction is valid or not.
     */
    public TxResult(String txId, boolean isValid) {
        this.txId = txId;
        this.isValid = isValid;
    }
}

