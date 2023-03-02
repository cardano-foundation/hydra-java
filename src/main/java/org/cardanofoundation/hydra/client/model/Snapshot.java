package org.cardanofoundation.hydra.client.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Snapshot {

    private int snapshotNumber;
    private Map<String, UTXO> utxo;

    @Override
    public String toString() {
        return "Snapshot{" +
                "snapshotNumber=" + snapshotNumber +
                ", utxo=" + utxo +
                '}';
    }

}
