package org.cardanofoundation.hydra.client;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
@Builder(builderMethodName = "hiddenBuilder")
public class HydraClientOptions {

    private final String serverURI;

    @Builder.Default
    private int fromSeq = 0;

    @Builder.Default
    private boolean history = true;

    @Nullable
    private TransactionFormat transactionFormat;

    public static HydraClientOptionsBuilder builder(String serverURI) {
        return hiddenBuilder().serverURI(serverURI);
    }

    public static HydraClientOptions createDefault(String serverURI) {
        return HydraClientOptions.builder(serverURI).build();
    }

    public enum TransactionFormat {
        CBOR, JSON
    }

}
