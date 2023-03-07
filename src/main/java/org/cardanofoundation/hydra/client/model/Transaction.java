package org.cardanofoundation.hydra.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    String id;
    boolean isValid;
    long fees;
    List<String> inputs;
    List<UTXO> outputs;

    // TODO witnesses

}
