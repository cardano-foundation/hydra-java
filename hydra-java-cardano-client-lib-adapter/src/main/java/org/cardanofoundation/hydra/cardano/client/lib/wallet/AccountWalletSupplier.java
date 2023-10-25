package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.account.Account;

public class AccountWalletSupplier implements WalletSupplier {

    private final Account account;

    public AccountWalletSupplier(Account account) {
        this.account = account;
    }

    @Override
    public Wallet getWallet() {
        return new SingleAccountWallet(account);
    }

}
