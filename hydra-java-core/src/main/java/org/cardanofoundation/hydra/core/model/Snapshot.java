package org.cardanofoundation.hydra.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.jetbrains.annotations.Nullable;

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

    @Nullable private Map<String, UTXO> utxo;

    private List<String> confirmedTransactions;

}
