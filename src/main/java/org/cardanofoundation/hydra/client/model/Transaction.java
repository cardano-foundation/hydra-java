package org.cardanofoundation.hydra.client.model;

import lombok.Getter;
import lombok.Setter;
import org.cardanofoundation.hydra.client.model.UTXO;

import java.util.List;

@Getter
@Setter
public class Transaction {

    String id;
    boolean isValid;
    long fees;
    List<String> inputs;
    List<UTXO> outputs;

    // TODO witnesses

}
