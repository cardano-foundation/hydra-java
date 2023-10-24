package org.cardanofoundation.hydra.cardano.client.lib.wallet;

/**
 * Accords to CIP-5 (https://cips.cardano.org/cips/cip5/)
 */
public enum KeyType {

    ACCOUNT_SIGNING_KEY_ED25519("acct_sk", true, false), // CIP-1852's account private key, Ed25519 private key
    ACCOUNT_VERIFICATION_KEY_ED25519("acct_vk", false, false), // CIP-1852's account public key, Ed25519 public key
    ACCOUNT_EXTENDED_SIGNING_KEY_ED25519("acct_xsk", true, true), // CIP-1852's extended account private key, Ed25519-bip32 extended private key
    ACCOUNT_EXTENDED_VERIFICATION_KEY_ED25519("acct_xvk", false, true), // CIP-1852's extended account public key, Ed25519 public key with chain code
    ACCOUNT_MULTISIG_SIGNING_KEY_ED25519("acct_shared_sk", true, false), // CIP-1854's account private key, Ed25519 private key
    ACCOUNT_MULTISIG_VERIFICATION_KEY_ED25519("acct_shared_vk", false, false), // CIP-1854's account public key, Ed25519 public key
    ACCOUNT_MULTISIG_EXTENDED_SIGNING_KEY_ED25519("acct_shared_xsk", true, true), // CIP-1854's extended account private key, Ed25519-bip32 extended private key
    ACCOUNT_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519("acct_shared_xvk", false, true), // CIP-1854's extended account public key, Ed25519 public key with chain code
    ADDRESS_SIGNING_KEY_ED25519("addr_sk", true, false), // CIP-1852's address signing key, Ed25519 private key
    ADDRESS_VERIFICATION_KEY_ED25519("addr_vk", false, false), // CIP-1852's address verification key Ed25519 public key
    ADDRESS_EXTENDED_SIGNING_KEY_ED25519("addr_xsk", true, true), // CIP-1852's address extended signing key Ed25519-bip32 extended private key
    ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519("addr_xvk", false, true), // CIP-1852's address extended verification key, Ed25519 public key with chain code
    ADDRESS_MULTISIG_SIGNING_KEY_ED25519("addr_shared_sk", true, false), // CIP-1854's address signing key, Ed25519 private key
    ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519("addr_shared_vk", false, false), // CIP-1854's address verification key, Ed25519 public key
    ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519("addr_shared_xsk", true, true), // CIP-1854's address extended signing key, Ed25519-bip32 extended private key
    ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519("addr_shared_xvk", false, true), // CIP-1854's address extended verification key, Ed25519 public key with chain code
    KES_SIGNING_KEY_ED25519("kes_sk", true, false), // KES signing key, KES signing key
    KES_VERIFICATION_KEY_ED25519("kes_vk", false, false), // KES verification key, KES verification key
    POLICY_SIGNING_KEY_ED25519("policy_sk", true, false), // CIP-1855's policy private key, Ed25519 private key
    POLICY_VERIFICATION_KEY_ED25519("policy_vk", false, false), // CIP-1855's policy public key, Ed25519 public key
    POOL_OPERATOR_SIGNING_KEY_ED25519("pool_sk", true, false), // Pool operator signing key, Ed25519 private key
    POOL_OPERATOR_VERIFICATION_KEY_ED25519("pool_vk", false, false), // Pool operator verification key, Ed25519 public key
    ROOT_SIGNING_KEY_ED25519("root_sk", true, false), // CIP-1852's root private key, Ed25519 private key
    ROOT_VERIFICATION_KEY_ED25519("root_vk", false, false), // CIP-1852's root public key, Ed25519 public key
    ROOT_EXTENDED_SIGNING_KEY_ED25519("root_xsk", true, true), // CIP-1852's extended root private key, Ed25519-bip32 extended private key
    ROOT_EXTENDED_VERIFICATION_KEY_ED25519("root_xvk", false, true), // CIP-1852's extended root public key, Ed25519 public key with chain code
    ROOT_MULTISIG_SIGNING_KEY_ED25519("root_shared_sk", true, false), // CIP-1854's root private key, Ed25519 private key
    ROOT_MULTISIG_VERIFICATION_KEY_ED25519("root_shared_vk", false, false), // CIP-1854's root public key, Ed25519 public key
    ROOT_MULTISIG_EXTENDED_SIGNING_KEY_ED25519("root_shared_xsk", true, true), // CIP-1854's extended root private key, Ed25519-bip32 extended private key
    ROOT_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519("root_shared_xvk", false, true), // CIP-1854's extended root public key, Ed25519 public key with chain code
    STAKE_ADDRESS_SIGNING_KEY_ED25519("stake_sk", true, false), // CIP-1852's stake address signing key, Ed25519 private key
    STAKE_ADDRESS_VERIFICATION_KEY_ED25519("stake_vk", false, false), // CIP-1852's stake address verification key, Ed25519 public key
    STAKE_ADDRESS_EXTENDED_SIGNING_KEY_ED25519("stake_xsk", true, true), // CIP-1852's extended stake address signing key, Ed25519-bip32 extended private key
    STAKE_ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519("stake_xvk", false, true), // CIP-1852's extended stake address verification key, Ed25519 public key with chain code
    STAKE_ADDRESS_MULTISIG_SIGNING_KEY_ED25519("stake_shared_sk", true, false), // CIP-1854's stake address signing key, Ed25519 private key
    STAKE_ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519("stake_shared_vk", false, false), // CIP-1854's stake address verification key, Ed25519 public key
    STAKE_ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519("stake_shared_xsk", true, true), // CIP-1854's extended stake address signing key, Ed25519-bip32 extended private key
    STAKE_ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519("stake_shared_xvk", false, true), // CIP-1854's extended stake address verification key, Ed25519 public key with chain code
    VRF_SIGNING_KEY_ED25519("vrf_sk", true, true), // VRF signing key, VRF signing key
    VRF_VERIFICATION_KEY_ED25519("vrf_vk", false, false); // VRF verification key, VRF verification key

    private final static String TESTNET_HRP_SUFFIX = "_test";

    private final String hrp;
    private final boolean isSigningKey;
    private final boolean isExtendedKey;

    public static KeyType toVerificationKeyType(final KeyType keyType) {
        return switch (keyType) {
            case ACCOUNT_SIGNING_KEY_ED25519 -> ACCOUNT_VERIFICATION_KEY_ED25519;
            case ACCOUNT_EXTENDED_SIGNING_KEY_ED25519 -> ACCOUNT_EXTENDED_VERIFICATION_KEY_ED25519;
            case ACCOUNT_MULTISIG_SIGNING_KEY_ED25519 -> ACCOUNT_MULTISIG_VERIFICATION_KEY_ED25519;
            case ACCOUNT_MULTISIG_EXTENDED_SIGNING_KEY_ED25519 -> ACCOUNT_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519;
            case ADDRESS_SIGNING_KEY_ED25519 -> ADDRESS_VERIFICATION_KEY_ED25519;
            case ADDRESS_EXTENDED_SIGNING_KEY_ED25519 -> ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519;
            case ADDRESS_MULTISIG_SIGNING_KEY_ED25519 -> ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519;
            case ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519 -> ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519;
            case KES_SIGNING_KEY_ED25519 -> KES_VERIFICATION_KEY_ED25519;
            case POLICY_SIGNING_KEY_ED25519 -> POLICY_VERIFICATION_KEY_ED25519;
            case POOL_OPERATOR_SIGNING_KEY_ED25519 -> POOL_OPERATOR_VERIFICATION_KEY_ED25519;
            case ROOT_SIGNING_KEY_ED25519 -> ROOT_VERIFICATION_KEY_ED25519;
            case ROOT_EXTENDED_SIGNING_KEY_ED25519 -> ROOT_EXTENDED_VERIFICATION_KEY_ED25519;
            case ROOT_MULTISIG_SIGNING_KEY_ED25519 -> ROOT_MULTISIG_VERIFICATION_KEY_ED25519;
            case ROOT_MULTISIG_EXTENDED_SIGNING_KEY_ED25519 -> ROOT_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519;
            case STAKE_ADDRESS_SIGNING_KEY_ED25519 -> STAKE_ADDRESS_VERIFICATION_KEY_ED25519;
            case STAKE_ADDRESS_EXTENDED_SIGNING_KEY_ED25519 -> STAKE_ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519;
            case STAKE_ADDRESS_MULTISIG_SIGNING_KEY_ED25519 -> STAKE_ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519;
            case STAKE_ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519 -> STAKE_ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519;
            case VRF_SIGNING_KEY_ED25519 -> VRF_VERIFICATION_KEY_ED25519;
            default -> keyType;
        };
    }

    public static KeyType toSigningKeyType(final KeyType keyType) {
        return switch (keyType) {
            case ACCOUNT_VERIFICATION_KEY_ED25519 -> ACCOUNT_SIGNING_KEY_ED25519;
            case ACCOUNT_EXTENDED_VERIFICATION_KEY_ED25519 -> ACCOUNT_EXTENDED_SIGNING_KEY_ED25519;
            case ACCOUNT_MULTISIG_VERIFICATION_KEY_ED25519 -> ACCOUNT_MULTISIG_SIGNING_KEY_ED25519;
            case ACCOUNT_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519 -> ACCOUNT_MULTISIG_EXTENDED_SIGNING_KEY_ED25519;
            case ADDRESS_VERIFICATION_KEY_ED25519 -> ADDRESS_SIGNING_KEY_ED25519;
            case ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519 -> ADDRESS_EXTENDED_SIGNING_KEY_ED25519;
            case ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519 -> ADDRESS_MULTISIG_SIGNING_KEY_ED25519;
            case ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519 -> ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519;
            case KES_VERIFICATION_KEY_ED25519 -> KES_SIGNING_KEY_ED25519;
            case POLICY_VERIFICATION_KEY_ED25519 -> POLICY_SIGNING_KEY_ED25519;
            case POOL_OPERATOR_VERIFICATION_KEY_ED25519 -> POOL_OPERATOR_SIGNING_KEY_ED25519;
            case ROOT_VERIFICATION_KEY_ED25519 -> ROOT_SIGNING_KEY_ED25519;
            case ROOT_EXTENDED_VERIFICATION_KEY_ED25519 -> ROOT_EXTENDED_SIGNING_KEY_ED25519;
            case ROOT_MULTISIG_VERIFICATION_KEY_ED25519 -> ROOT_MULTISIG_SIGNING_KEY_ED25519;
            case ROOT_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519 -> ROOT_MULTISIG_EXTENDED_SIGNING_KEY_ED25519;
            case STAKE_ADDRESS_VERIFICATION_KEY_ED25519 -> STAKE_ADDRESS_SIGNING_KEY_ED25519;
            case STAKE_ADDRESS_EXTENDED_VERIFICATION_KEY_ED25519 -> STAKE_ADDRESS_EXTENDED_SIGNING_KEY_ED25519;
            case STAKE_ADDRESS_MULTISIG_VERIFICATION_KEY_ED25519 -> STAKE_ADDRESS_MULTISIG_SIGNING_KEY_ED25519;
            case STAKE_ADDRESS_MULTISIG_EXTENDED_VERIFICATION_KEY_ED25519 -> STAKE_ADDRESS_MULTISIG_EXTENDED_SIGNING_KEY_ED25519;
            case VRF_VERIFICATION_KEY_ED25519 -> VRF_SIGNING_KEY_ED25519;
            default -> keyType;
        };
    }

    KeyType(final String hrp, final boolean isSigningKey, final boolean isExtendedKey) {
        this.hrp = hrp;
        this.isSigningKey = isSigningKey;
        this.isExtendedKey = isExtendedKey;
    }

    static KeyType fromHrp(final String hrp) {
        for (KeyType keyType : KeyType.values()) {
            if (hrp.equals(keyType.getHrp()) || hrp.equals(keyType.getTestnetHrp())) {
                return keyType;
            }
        }
        throw new IllegalArgumentException("No matching key type for hrp of given bech32 value.");
    }

    public final String getHrp() {
        return this.hrp;
    }

    public final String getTestnetHrp() {
        return this.hrp + TESTNET_HRP_SUFFIX;
    }

    public final boolean isSigningKey() {
        return isSigningKey;
    }

    public final boolean isExtendedKey() {
        return isExtendedKey;
    }

}