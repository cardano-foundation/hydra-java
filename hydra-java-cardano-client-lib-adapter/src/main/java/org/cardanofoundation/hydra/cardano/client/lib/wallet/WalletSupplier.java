package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.exception.CborSerializationException;

public interface WalletSupplier {

    Wallet getWallet() throws CborSerializationException;

}
