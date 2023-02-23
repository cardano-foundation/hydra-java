package org.cardanofoundation.hydra.client.model.base;

import java.util.Arrays;
import java.util.Optional;

public enum MethodType {

    QUERY("Query"),

    SUBMIT_TX("SubmitTx");

    private final String value;

    MethodType(String value) {
        this.value = value;
    }

    public static Optional<MethodType> convert(String type) {
        return Arrays.stream(values()).filter(mt -> mt.value.equals(type)).findFirst();
    }

    public String getValue() {
        return value;
    }
}
