package org.cardanofoundation.hydra.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
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
