package org.cardanofoundation.hydra.client.client.utils;

import com.bloxbean.cardano.client.function.TxSigner;

public interface OperatorAccountSupplier {

    String getOperatorAddress();

    TxSigner getTxSigner();

}
