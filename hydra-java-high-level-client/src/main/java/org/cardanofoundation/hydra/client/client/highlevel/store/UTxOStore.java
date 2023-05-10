package org.cardanofoundation.hydra.client.client.highlevel.store;

import org.cardanofoundation.hydra.core.model.UTXO;

import java.util.Map;

public interface UTxOStore {

    //@NotNull
    Map<String, UTXO> getLatestUTxO();

    void storeLatestUtxO(Map<String, UTXO> utxo);

}
