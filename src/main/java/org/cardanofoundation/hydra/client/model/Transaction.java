package org.cardanofoundation.hydra.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class Transaction {

    /** Transaction ID */
    String id;

    /** whether this transaction is valid or not */
    Boolean isValid;

    TxBody txBody;

    // TODO
    // String witnesses
    // String auxiliaryData

}
