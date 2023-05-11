package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.function.TxSigner;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class HydraOperator {

    private final String address;

    private final TxSigner txSigner;

    @Override
    public String toString() {
        return "HydraOperator{" +
                "address='" + address + '\'' +
                '}';
    }

}
