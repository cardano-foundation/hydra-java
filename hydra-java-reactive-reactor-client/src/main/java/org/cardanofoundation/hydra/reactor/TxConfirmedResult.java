package org.cardanofoundation.hydra.reactor;

/**
 * Represents a confirmed transaction by all Hydra head participants.
 *
 * @param txId
 */
public record TxConfirmedResult(String txId) {
}
