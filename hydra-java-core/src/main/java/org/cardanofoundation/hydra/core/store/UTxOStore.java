package org.cardanofoundation.hydra.core.store;

import org.cardanofoundation.hydra.core.model.UTXO;

import java.util.Map;

public interface UTxOStore {

    //@NotNull
    Map<String, UTXO> getLatestUTxO();

    void storeLatestUtxO(Map<String, UTXO> utxo);

}
