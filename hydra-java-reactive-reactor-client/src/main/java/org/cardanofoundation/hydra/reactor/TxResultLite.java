package org.cardanofoundation.hydra.reactor;

import lombok.Getter;

@Getter
public class TxResultLite {

    private final String txId;

    public TxResultLite(String txId) {
        this.txId = txId;
    }

}
