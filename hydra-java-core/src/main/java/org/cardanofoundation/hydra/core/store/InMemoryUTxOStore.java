package org.cardanofoundation.hydra.core.store;

import lombok.NoArgsConstructor;
import org.cardanofoundation.hydra.core.model.UTXO;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor
public class InMemoryUTxOStore implements UTxOStore {

    private final AtomicReference<Map<String, UTXO>> reference = new AtomicReference<>(new HashMap<>());

    public InMemoryUTxOStore(Map<String, UTXO> utxoMap) {
        reference.set(utxoMap);
    }

    @Override
    public Map<String, UTXO> getLatestUTxO() {
        return reference.get();
    }

    @Override
    public void storeLatestUtxO(Map<String, UTXO> utxo) {
        reference.set(utxo);
    }

}
