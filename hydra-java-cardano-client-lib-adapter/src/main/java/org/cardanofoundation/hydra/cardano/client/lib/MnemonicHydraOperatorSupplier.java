package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.account.Account;

public class MnemonicHydraOperatorSupplier implements HydraOperatorSupplier {

    private final Account account;

    public MnemonicHydraOperatorSupplier(Account account) {
        this.account = account;
    }

    @Override
    public HydraOperator getOperator() {
        return new HydraOperator(account.baseAddress(), account::sign);
    }

}
