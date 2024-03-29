package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public enum KeyTextEnvelopeType {
    PAYMENT_EXTENDED_SIGNING_KEY_SHELLEY_ED25519_BIP32("PaymentExtendedSigningKeyShelley_ed25519_bip32", "Payment Signing Key", KeyType.ACCOUNT_EXTENDED_SIGNING_KEY_ED25519),
    PAYMENT_EXTENDED_VERIFICATION_KEY_SHELLEY_ED25519_BIP32("PaymentExtendedVerificationKeyShelley_ed25519_bip32", "Payment Verification Key", KeyType.ACCOUNT_EXTENDED_VERIFICATION_KEY_ED25519),
    PAYMENT_SIGNING_KEY_SHELLEY_ED25519("PaymentSigningKeyShelley_ed25519", "Payment Signing Key", KeyType.ACCOUNT_SIGNING_KEY_ED25519),
    PAYMENT_VERIFICATION_KEY_SHELLEY_ED25519("PaymentVerificationKeyShelley_ed25519", "Payment Verification Key", KeyType.ACCOUNT_VERIFICATION_KEY_ED25519),
    PAYMENT_SIGNING_KEY_BYRON_ED25519_BIP32("PaymentSigningKeyByron_ed25519_bip32", "Payment Signing Key", KeyType.ACCOUNT_EXTENDED_SIGNING_KEY_ED25519),
    PAYMENT_VERIFICATION_KEY_BYRON_ED25519_BIP32("PaymentVerificationKeyByron_ed25519_bip32", "Payment Verification Key", KeyType.ACCOUNT_EXTENDED_VERIFICATION_KEY_ED25519),
    KES_SIGNING_KEY_ED25519_KES_26("KesSigningKey_ed25519_kes_2^6", "KES Signing Key", KeyType.KES_SIGNING_KEY_ED25519),
    KES_VERIFICATION_KEY_ED25519_KES_2("KesVerificationKey_ed25519_kes_2", "KES Verification Key", KeyType.KES_VERIFICATION_KEY_ED25519),
    STAKE_ADDRESS_SIGNING_KEY_ED25519("StakeSigningKeyShelley_ed25519", "Stake Signing Key", KeyType.STAKE_ADDRESS_SIGNING_KEY_ED25519),
    STAKE_ADDRESS_VERIFICATION_KEY_ED25519("StakeVerificationKeyShelley_ed25519", "Stake Verification Key", KeyType.STAKE_ADDRESS_VERIFICATION_KEY_ED25519),
    STAKE_POOL_SIGNING_KEY_ED25519("StakePoolSigningKey_ed25519", "Stake Pool Operator Signing Key", KeyType.POOL_OPERATOR_SIGNING_KEY_ED25519),
    STAKE_POOL_VERIFICATION_KEY_ED25519("StakePoolVerificationKey_ed25519", "Stake Pool Operator Verification Key", KeyType.POOL_OPERATOR_VERIFICATION_KEY_ED25519),
    VRF_SIGNING_KEY_PRAOS_VRF("VrfSigningKey_PraosVRF", "VRF Signing Key", KeyType.VRF_SIGNING_KEY_ED25519),
    VRF_VERIFICATION_KEY_PRAOS_VRF("VrfVerificationKey_PraosVRF", "VRF Verification Key", KeyType.VRF_VERIFICATION_KEY_ED25519);

    private static final Map<KeyTextEnvelopeType, List<KeyType>> KEY_TYPE_COMPATIBILITY_MATRIX = Map.ofEntries(
            entry(PAYMENT_EXTENDED_SIGNING_KEY_SHELLEY_ED25519_BIP32, Arrays.asList(KeyType.ADDRESS_EXTENDED_SIGNING_KEY_ED25519, KeyType.ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519, KeyType.ROOT_EXTENDED_SIGNING_KEY_ED25519, KeyType.ROOT_MULTISIG_EXTENDED_SIGNING_KEY_ED25519, KeyType.STAKE_ADDRESS_EXTENDED_SIGNING_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519)),
            entry(PAYMENT_EXTENDED_VERIFICATION_KEY_SHELLEY_ED25519_BIP32, Arrays.asList(KeyType.ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519, KeyType.ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519, KeyType.ROOT_EXTENDED_VERIFICATION_KEY_ED25519, KeyType.ROOT_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519, KeyType.STAKE_ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519)),
            entry(PAYMENT_SIGNING_KEY_SHELLEY_ED25519, Arrays.asList(KeyType.ACCOUNT_SIGNING_KEY_ED25519, KeyType.ACCOUNT_MULTISIG_SIGNING_KEY_ED25519, KeyType.ADDRESS_SIGNING_KEY_ED25519, KeyType.ADDRESS_MULTISIG_SIGNING_KEY_ED25519, KeyType.POLICY_SIGNING_KEY_ED25519, KeyType.POOL_OPERATOR_SIGNING_KEY_ED25519, KeyType.ROOT_SIGNING_KEY_ED25519, KeyType.ROOT_MULTISIG_SIGNING_KEY_ED25519, KeyType.STAKE_ADDRESS_SIGNING_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_SIGNING_KEY_ED25519)),
            entry(PAYMENT_VERIFICATION_KEY_SHELLEY_ED25519, Arrays.asList(KeyType.ACCOUNT_VERIFICATION_KEY_ED25519, KeyType.ACCOUNT_MULTISIG_VERIFICATION_KEY_ED25519, KeyType.ADDRESS_VERIFICATION_KEY_ED25519, KeyType.ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519, KeyType.POLICY_VERIFICATION_KEY_ED25519, KeyType.POOL_OPERATOR_VERIFICATION_KEY_ED25519, KeyType.ROOT_VERIFICATION_KEY_ED25519, KeyType.ROOT_MULTISIG_VERIFICATION_KEY_ED25519, KeyType.STAKE_ADDRESS_VERIFICATION_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519)),
            entry(PAYMENT_SIGNING_KEY_BYRON_ED25519_BIP32, List.of()),
            entry(PAYMENT_VERIFICATION_KEY_BYRON_ED25519_BIP32, List.of()),
            entry(KES_SIGNING_KEY_ED25519_KES_26, List.of(KeyType.KES_SIGNING_KEY_ED25519)),
            entry(KES_VERIFICATION_KEY_ED25519_KES_2, List.of(KeyType.KES_VERIFICATION_KEY_ED25519)),
            entry(STAKE_ADDRESS_SIGNING_KEY_ED25519, Arrays.asList(KeyType.STAKE_ADDRESS_SIGNING_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_SIGNING_KEY_ED25519)),
            entry(STAKE_ADDRESS_VERIFICATION_KEY_ED25519, Arrays.asList(KeyType.STAKE_ADDRESS_VERIFICATION_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519)),
            entry(STAKE_POOL_SIGNING_KEY_ED25519, Arrays.asList(KeyType.ACCOUNT_SIGNING_KEY_ED25519, KeyType.ACCOUNT_MULTISIG_SIGNING_KEY_ED25519, KeyType.ADDRESS_SIGNING_KEY_ED25519, KeyType.ADDRESS_MULTISIG_SIGNING_KEY_ED25519, KeyType.POLICY_SIGNING_KEY_ED25519, KeyType.POOL_OPERATOR_SIGNING_KEY_ED25519, KeyType.ROOT_SIGNING_KEY_ED25519, KeyType.ROOT_MULTISIG_SIGNING_KEY_ED25519, KeyType.STAKE_ADDRESS_SIGNING_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_SIGNING_KEY_ED25519)),
            entry(STAKE_POOL_VERIFICATION_KEY_ED25519, Arrays.asList(KeyType.ACCOUNT_VERIFICATION_KEY_ED25519, KeyType.ACCOUNT_MULTISIG_VERIFICATION_KEY_ED25519, KeyType.ADDRESS_VERIFICATION_KEY_ED25519, KeyType.ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519, KeyType.POLICY_VERIFICATION_KEY_ED25519, KeyType.POOL_OPERATOR_VERIFICATION_KEY_ED25519, KeyType.ROOT_VERIFICATION_KEY_ED25519, KeyType.ROOT_MULTISIG_VERIFICATION_KEY_ED25519, KeyType.STAKE_ADDRESS_VERIFICATION_KEY_ED25519, KeyType.STAKE_ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519)),
            entry(VRF_SIGNING_KEY_PRAOS_VRF, List.of(KeyType.VRF_SIGNING_KEY_ED25519)),
            entry(VRF_VERIFICATION_KEY_PRAOS_VRF, List.of(KeyType.VRF_VERIFICATION_KEY_ED25519))
    );

    private final String type;
    private final String description;
    private final KeyType defaultKeyType;

    KeyTextEnvelopeType(final String type, final String description, final KeyType defaultKeyType) {
        this.type = type;
        this.description = description;
        this.defaultKeyType = defaultKeyType;
    }

    final public String getType() {
        return this.type;
    }

    final public String getDescription() {
        return this.description;
    }

    final public KeyType getDefaultKeyType() {
        return this.defaultKeyType;
    }

    public static boolean isCompatibleWith(final KeyTextEnvelopeType envelopeKeyType, final KeyType keyType) {
        final List<KeyType> supportedKeyTypes = KeyTextEnvelopeType.KEY_TYPE_COMPATIBILITY_MATRIX.getOrDefault(envelopeKeyType, List.of());

        return supportedKeyTypes.contains(keyType);
    }

    public static KeyTextEnvelopeType fromTypeValue(final String typeValue) {
        for (final KeyTextEnvelopeType keyTextEnvelopeType : KeyTextEnvelopeType.values()) {
            if (typeValue.equals(keyTextEnvelopeType.getType())) {
                return keyTextEnvelopeType;
            }
        }

        throw new IllegalArgumentException(String.format("Given type value '%s' not supported", typeValue));
    }
}