package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.crypto.Keys;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import lombok.RequiredArgsConstructor;
import lombok.val;

import static org.cardanofoundation.hydra.cardano.client.lib.wallet.KeyTextEnvelopeType.PAYMENT_SIGNING_KEY_SHELLEY_ED25519;
import static org.cardanofoundation.hydra.cardano.client.lib.wallet.KeyTextEnvelopeType.PAYMENT_VERIFICATION_KEY_SHELLEY_ED25519;

@RequiredArgsConstructor
public class PlainKeyTextEnvelopeWalletSupplier implements WalletSupplier {

    private final KeyTextEnvelope signingKeyEnvelope;
    private final KeyTextEnvelope verificationKeyEnvelope;

    @Override
    public Wallet getWallet() {
        if (signingKeyEnvelope.getType() == PAYMENT_SIGNING_KEY_SHELLEY_ED25519
                && verificationKeyEnvelope.getType() == PAYMENT_VERIFICATION_KEY_SHELLEY_ED25519) {

            val secretKey = new SecretKey(signingKeyEnvelope.getCborHex());
            val verificationKey = new VerificationKey(verificationKeyEnvelope.getCborHex());

            return new PlainWallet(new Keys(secretKey, verificationKey));
        }

        throw new IllegalArgumentException("Unsupported key type, type:" + signingKeyEnvelope.getType());
    }

}
