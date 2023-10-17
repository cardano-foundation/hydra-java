package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import org.cardanofoundation.hydra.cardano.client.lib.utils.MoreAddress;

import static com.bloxbean.cardano.client.crypto.KeyGenUtil.getPublicKeyFromPrivateKey;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;

/**
 * A static secret key supplier.
 */
public class StaticSecretKeySupplier implements CardanoOperatorSupplier {

    private final SecretKey secretKey;
    private final Network network;

    private final VerificationKey verificationKey;

    /**
     * Constructor.
     *
     * @param cborHex - the payment key hex (skey)
     * @param network - the network
     * @throws CborSerializationException
     */
    public StaticSecretKeySupplier(String cborHex,
                                   Network network) throws CborSerializationException {
        this.secretKey = new SecretKey(cborHex);
        this.network = network;
        this.verificationKey = getPublicKeyFromPrivateKey(secretKey);
    }

    @Override
    public CardanoOperator getOperator() {
        var address = MoreAddress.getBech32AddressFromVerificationKey(verificationKey.getCborHex(), network);

        return new CardanoOperator(address, verificationKey, secretKey, signerFrom(secretKey));
    }

}
