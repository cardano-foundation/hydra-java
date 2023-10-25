package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.net.URL;

@AllArgsConstructor
public class JsonUriWalletSupplierFactory {

    private final URL signingKeyPath;
    private final URL verificationKeyPath;
    private final ObjectMapper objectMapper;

    public WalletSupplier loadWallet() throws IOException {
        var signingKeyEnvelope = objectMapper.readValue(signingKeyPath, KeyTextEnvelope.class);
        var verificationKeyEnvelope = objectMapper.readValue(verificationKeyPath, KeyTextEnvelope.class);

        return new KeyTextEnvelopeWalletSupplier(signingKeyEnvelope, verificationKeyEnvelope);
    }

}
