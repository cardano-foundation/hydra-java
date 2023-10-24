package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import lombok.Getter;
import lombok.ToString;
import org.cardanofoundation.hydra.cardano.client.lib.utils.MoreAddress;

@Getter
@ToString
public class Wallet {

    private final SecretKey secretKey;
    private final VerificationKey verificationKey;

    public Wallet(SecretKey secretKey,
                  VerificationKey verificationKey) {
        if (secretKey == null) {
            throw new IllegalArgumentException("Secret key cannot be null");
        }
        if (verificationKey == null) {
            throw new IllegalArgumentException("Verification key cannot be null");
        }
        this.secretKey = secretKey;
        this.verificationKey = verificationKey;
    }

    /**
     * Get the address of the wallet in bech32 format
     * @return
     */
    public String getAddress(Network network) {
        return MoreAddress.getBech32AddressFromVerificationKey(verificationKey.getCborHex(), network);
    }

    public TxSigner getTxSigner() {
        return SignerProviders.signerFrom(secretKey);
    }

}
