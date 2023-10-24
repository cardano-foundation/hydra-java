package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import lombok.val;

/**
 * A static secret key supplier.
 */
public class KeyTextEnvelopeWalletSupplier implements WalletSupplier {

    private final Wallet wallet;

    public KeyTextEnvelopeWalletSupplier(KeyTextEnvelope signingKeyEnvelope,
                                         KeyTextEnvelope verificationKeyEnvelope) {
        if (signingKeyEnvelope == null) {
            throw new IllegalArgumentException("signingKeyEnvelope cannot be null");
        }
        if (verificationKeyEnvelope == null) {
            throw new IllegalArgumentException("verificationKeyEnvelope cannot be null");
        }

        val secretKey = new SecretKey(signingKeyEnvelope.getCborHex());
        val verificationKey = new VerificationKey(verificationKeyEnvelope.getCborHex());

        this.wallet = new Wallet(secretKey, verificationKey);
    }

    @Override
    public Wallet getWallet() {
        return wallet;
    }

}
