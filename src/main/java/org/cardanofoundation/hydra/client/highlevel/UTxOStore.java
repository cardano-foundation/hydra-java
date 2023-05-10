package org.cardanofoundation.hydra.client.highlevel;

import org.cardanofoundation.hydra.client.model.UTXO;

import java.util.Map;

public interface UTxOStore {

    //@NotNull
    Map<String, UTXO> getLatestUTxO();

    void storeLatestUtxO(Map<String, UTXO> utxo);

}
