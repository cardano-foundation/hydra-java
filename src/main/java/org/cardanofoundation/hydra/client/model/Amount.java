package org.cardanofoundation.hydra.client.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
public class Amount {

    private String unit;
    private BigInteger quantity;
}
