package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.function.TxSigner;

public interface Wallet {

    String getBech32Address(Network network);

    TxSigner getTxSigner();

    SecretKey getSecretKey();

    VerificationKey getVerificationKey();

}
