package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.hydra.core.HydraException;

import java.io.IOException;
import java.io.InputStream;

public class JacksonClasspathSecretKeySupplierHydra implements HydraOperatorSupplier {

    private final SecretKey secretKey;

    private final VerificationKey verificationKey;

    public JacksonClasspathSecretKeySupplierHydra(ObjectMapper objectMapper, String classpathLink) throws CborSerializationException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpathLink)) {
            var tree = objectMapper.readTree(is);

            this.secretKey = new SecretKey(tree.get("cborHex").asText());
            this.verificationKey = KeyGenUtil.getPublicKeyFromPrivateKey(secretKey);
        } catch (IOException e) {
            throw new HydraException(e);
        }
    }

    @Override
    public HydraOperator getOperator() {
        return new HydraOperator(getAddressFromVerificationKey(verificationKey.getCborHex()), SignerProviders.signerFrom(secretKey));
    }

    private static String getAddressFromVerificationKey(String vkCborHex) {
        VerificationKey vk = new VerificationKey(vkCborHex);
        HdPublicKey hdPublicKey = new HdPublicKey();
        hdPublicKey.setKeyData(vk.getBytes());
        Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());

        return address.toBech32();
    }

}
