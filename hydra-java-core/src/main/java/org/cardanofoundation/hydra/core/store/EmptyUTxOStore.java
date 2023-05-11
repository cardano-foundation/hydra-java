package org.cardanofoundation.hydra.core.store;

import org.cardanofoundation.hydra.core.model.UTXO;

import java.util.Map;

public class EmptyUTxOStore implements UTxOStore {

    @Override
    public Map<String, UTXO> getLatestUTxO() {
        return Map.of();
    }

    @Override
    public void storeLatestUtxO(Map<String, UTXO> utxo) {
        //noop
    }
}
