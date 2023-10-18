package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import lombok.SneakyThrows;

public class AccountCardanoOperatorSupplier implements CardanoOperatorSupplier {

    private final Account account;

    public AccountCardanoOperatorSupplier(Account account) {
        this.account = account;
    }

    @SneakyThrows
    @Override
    public CardanoOperator getOperator() {
        return new CardanoOperator(
                account.baseAddress(),
                VerificationKey.create(account.publicKeyBytes()),
                SecretKey.create(account.privateKeyBytes()),
                account::sign);
    }

}
