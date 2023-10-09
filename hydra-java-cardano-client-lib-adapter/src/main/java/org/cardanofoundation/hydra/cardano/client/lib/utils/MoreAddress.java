package org.cardanofoundation.hydra.cardano.client.lib.utils;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;

import static com.bloxbean.cardano.client.address.AddressProvider.getEntAddress;

public final class MoreAddress {

    public static String getBech32AddressFromVerificationKey(String vkCborHex,
                                                              Network network) {
        VerificationKey vk = new VerificationKey(vkCborHex);
        HdPublicKey hdPublicKey = new HdPublicKey();
        hdPublicKey.setKeyData(vk.getBytes());
        Address address = getEntAddress(hdPublicKey, network);

        return address.toBech32();
    }

}
