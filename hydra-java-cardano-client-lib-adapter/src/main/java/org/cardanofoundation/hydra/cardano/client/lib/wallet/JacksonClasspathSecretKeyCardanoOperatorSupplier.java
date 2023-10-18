package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import org.cardanofoundation.hydra.cardano.client.lib.utils.MoreAddress;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.CardanoOperator;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.CardanoOperatorSupplier;
import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.utils.MoreJson;

import java.io.IOException;
import java.io.InputStream;

import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;

public class JacksonClasspathSecretKeyCardanoOperatorSupplier implements CardanoOperatorSupplier {

    private final SecretKey secretKey;

    private final VerificationKey verificationKey;

    private final Network network;

    public JacksonClasspathSecretKeyCardanoOperatorSupplier(String classpathLink,
                                                            Network network) throws CborSerializationException {
        this.network = network;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpathLink)) {
            var tree = MoreJson.read(is);

            this.secretKey = new SecretKey(tree.get("cborHex").asText());
            this.verificationKey = KeyGenUtil.getPublicKeyFromPrivateKey(secretKey);
        } catch (IOException e) {
            throw new HydraException(e);
        }
    }

    @Override
    public CardanoOperator getOperator() {
        var address = MoreAddress.getBech32AddressFromVerificationKey(verificationKey.getCborHex(), network);

        return new CardanoOperator(address, verificationKey, secretKey, signerFrom(secretKey));
    }

}
