package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.function.TxSigner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class CardanoOperator {

    private final String address;

    private final VerificationKey verificationKey;

    private final SecretKey secretKey;

    private final TxSigner txSigner;

}
