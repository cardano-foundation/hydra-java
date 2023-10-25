package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.crypto.Keys;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static org.cardanofoundation.hydra.cardano.client.lib.utils.MoreAddress.getBech32AddressFromVerificationKey;

@RequiredArgsConstructor
@Getter
@ToString
public class PlainWallet implements Wallet {

    private final Keys keys;

    /**
     * Get the address of the wallet in bech32 format
     * @return
     */
    public String getBech32Address(Network network) {
        return getBech32AddressFromVerificationKey(keys.getVkey().getCborHex(), network);
    }

    public TxSigner getTxSigner() {
        return SignerProviders.signerFrom(keys.getSkey());
    }

    @Override
    public SecretKey getSecretKey()  {
        return new SecretKey(keys.getSkey().getCborHex());
    }

    @Override
    public VerificationKey getVerificationKey() {
        return new VerificationKey(keys.getVkey().getCborHex());
    }

}
