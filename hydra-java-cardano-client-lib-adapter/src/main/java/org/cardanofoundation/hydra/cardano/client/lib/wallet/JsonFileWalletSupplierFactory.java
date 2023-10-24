package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@AllArgsConstructor
public class JsonFileWalletSupplierFactory {

    private final String signingKeyPath;
    private final String verificationKeyPath;
    private final ObjectMapper objectMapper;

    public WalletSupplier loadWallet() throws IOException {
        String signingKeyContent = Files.readString(Path.of(signingKeyPath));
        String verificationKeyContent = Files.readString(Path.of(verificationKeyPath));

        var signingKeyEnvelope = objectMapper.readValue(signingKeyContent, KeyTextEnvelope.class);
        var verificationKeyEnvelope = objectMapper.readValue(verificationKeyContent, KeyTextEnvelope.class);

        return new KeyTextEnvelopeWalletSupplier(signingKeyEnvelope, verificationKeyEnvelope);
    }

}
