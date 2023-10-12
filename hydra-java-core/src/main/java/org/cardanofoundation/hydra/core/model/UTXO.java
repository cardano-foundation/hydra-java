package org.cardanofoundation.hydra.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.math.BigInteger;
import java.util.Map;
import java.util.StringJoiner;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UTXO {

    String address;
    Map<String, BigInteger> value;
    String datum;
    String datumhash;
    JsonNode inlineDatum;
    String inlineDatumhash;
    String referenceScript;


    @Override
    public String toString() {
        var joiner = new StringJoiner(", ", "UTXO {", "} ");

        if (address != null) {
            joiner.add("Address: " + address);
        }

        if (value != null && !value.isEmpty()) {
            joiner.add("Value: " + value);
        }

        if (datum != null) {
            joiner.add("Datum: " + datum);
        }

        if (datumhash != null) {
            joiner.add("DatumHash: " + datumhash);
        }

        if (inlineDatum != null) {
            joiner.add("InlineDatum: " + inlineDatum);
        }

        if (inlineDatumhash != null) {
            joiner.add("InlineDatumHash: `" + inlineDatumhash);
        }

        if (referenceScript != null) {
            joiner.add("ReferenceScript: " + referenceScript);
        }

        return joiner.toString();
    }

}
