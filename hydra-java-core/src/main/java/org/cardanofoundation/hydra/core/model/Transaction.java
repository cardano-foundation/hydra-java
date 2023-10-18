package org.cardanofoundation.hydra.core.model;

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

}
