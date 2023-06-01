package org.cardanofoundation.hydra.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.math.BigInteger;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UTXO {
    String address;
    Map<String, BigInteger> value;
    String datum;
    String datumhash;
    JsonNode inlineDatum;
    String inlineDatumhash;
    String referenceScript;
}
