package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class SingleAccountWallet implements Wallet {

    private final Account account;

    @Override
    public String getBech32Address(Network network) {
        return account.baseAddress();
    }

    @Override
    public TxSigner getTxSigner() {
        return SignerProviders.signerFrom(account);
    }

    @Override
    @SneakyThrows
    public SecretKey getSecretKey() {
        return SecretKey.create(account.privateKeyBytes());
    }

    @Override
    @SneakyThrows
    public VerificationKey getVerificationKey() {
        return VerificationKey.create(account.publicKeyBytes());
    }

}
