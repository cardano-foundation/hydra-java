package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import lombok.val;

import static org.cardanofoundation.hydra.cardano.client.lib.wallet.KeyTextEnvelopeType.PAYMENT_EXTENDED_SIGNING_KEY_SHELLEY_ED25519_BIP32;
import static org.cardanofoundation.hydra.cardano.client.lib.wallet.KeyTextEnvelopeType.PAYMENT_EXTENDED_VERIFICATION_KEY_SHELLEY_ED25519_BIP32;

public class AccountWalletSupplier implements WalletSupplier {

    private final Account account;

    public AccountWalletSupplier(Account account) {
        this.account = account;
    }

    @Override
    public Wallet getWallet() throws CborSerializationException {
        val secretKey = SecretKey.create(account.privateKeyBytes());
        val verificationKey = VerificationKey.create(account.publicKeyBytes());

        secretKey.setType(PAYMENT_EXTENDED_SIGNING_KEY_SHELLEY_ED25519_BIP32.getType());
        secretKey.setDescription(PAYMENT_EXTENDED_SIGNING_KEY_SHELLEY_ED25519_BIP32.getDescription());

        verificationKey.setType(PAYMENT_EXTENDED_VERIFICATION_KEY_SHELLEY_ED25519_BIP32.getType());
        verificationKey.setDescription(PAYMENT_EXTENDED_VERIFICATION_KEY_SHELLEY_ED25519_BIP32.getDescription());

        return new Wallet(secretKey, verificationKey);
    }

}
