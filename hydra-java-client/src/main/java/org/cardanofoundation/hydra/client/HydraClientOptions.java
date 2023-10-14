package org.cardanofoundation.hydra.client;

import lombok.Builder;
import lombok.Getter;
import org.cardanofoundation.hydra.core.store.EmptyUTxOStore;
import org.cardanofoundation.hydra.core.store.UTxOStore;
import org.jetbrains.annotations.Nullable;

@Getter
@Builder(builderMethodName = "hiddenBuilder")
public class HydraClientOptions {

    private final String serverURI;

    @Builder.Default
    private boolean history = false;

    @Builder.Default
    private UTxOStore utxoStore = new EmptyUTxOStore();

    @Builder.Default
    private boolean snapshotUtxo = false;

    /**
     * Hydra internal consensus level errors are not propagated to the developer
     */
    @Builder.Default
    private boolean doNotPropagateLowLevelFailures = true;

    @Builder.Default
    private TransactionFormat transactionFormat = TransactionFormat.CBOR;

    /**
     *
     * @param serverURI
     * @return
     */
    public static HydraClientOptionsBuilder builder(String serverURI) {
        return hiddenBuilder().serverURI(serverURI);
    }

    /**
     *
     * @param serverURI
     * @return
     */
    public static HydraClientOptions createDefault(String serverURI) {
        return HydraClientOptions.builder(serverURI).build();
    }

    public enum TransactionFormat {
        CBOR, JSON
    }

}
