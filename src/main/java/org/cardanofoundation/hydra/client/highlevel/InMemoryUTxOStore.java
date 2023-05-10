package org.cardanofoundation.hydra.client.highlevel;

import org.cardanofoundation.hydra.client.model.UTXO;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryUTxOStore implements UTxOStore {

    private final AtomicReference<Map<String, UTXO>> reference = new AtomicReference<>(new HashMap<>());

    @Override
    public Map<String, UTXO> getLatestUTxO() {
        return reference.get();
    }

    @Override
    public void storeLatestUtxO(Map<String, UTXO> utxo) {
        reference.set(utxo);
    }

}
