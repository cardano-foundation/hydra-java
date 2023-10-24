package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
public class JsonClasspathWalletSupplierFactory {

    private final String signingKeyPath;
    private final String verificationKeyPath;
    private final ObjectMapper objectMapper;

    public WalletSupplier loadWallet() throws IOException {
        var skUrl = JsonClasspathWalletSupplierFactory.class.getClassLoader()
                .getResource(signingKeyPath);

        var vkUrl = JsonClasspathWalletSupplierFactory.class.getClassLoader()
                .getResource(verificationKeyPath);

        var signingKeyEnvelope = objectMapper.readValue(skUrl, KeyTextEnvelope.class);
        var verificationKeyEnvelope = objectMapper.readValue(vkUrl, KeyTextEnvelope.class);

        return new KeyTextEnvelopeWalletSupplier(signingKeyEnvelope, verificationKeyEnvelope);
    }

}
