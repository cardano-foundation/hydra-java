package org.cardanofoundation.hydra.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Snapshot {

    private int snapshotNumber;
    private Map<String, UTXO> utxo;

    private List<Transaction> confirmedTransactions;

}
