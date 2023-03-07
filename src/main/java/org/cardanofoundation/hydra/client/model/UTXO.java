package org.cardanofoundation.hydra.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UTXO {
    String address;
    Map<String, BigInteger> value;
    String datum;
    String datumhash;
    JsonNode inlineDatum;
    String inlineDatumhash;
    String referenceScript;
}
