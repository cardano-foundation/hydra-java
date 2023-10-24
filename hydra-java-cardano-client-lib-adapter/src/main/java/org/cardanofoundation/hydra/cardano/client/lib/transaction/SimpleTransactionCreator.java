package org.cardanofoundation.hydra.cardano.client.lib.transaction;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.Wallet;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;

@RequiredArgsConstructor
public class SimpleTransactionCreator {

    private final UtxoSupplier utxoSupplier;

    private final ProtocolParamsSupplier protocolParamsSupplier;

    private final Network network;

    public byte[] simpleTransfer(Wallet sender,
                                 Wallet receiver,
                                 int adaAmount) throws CborSerializationException {
        var senderAddress = sender.getAddress(network);
        var receiverAddress = receiver.getAddress(network);

        var output = Output.builder()
                .address(receiverAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(adaAmount))
                .build();

        var txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(senderAddress, senderAddress))
                .andThen(balanceTx(senderAddress, 1));

        val txBuilderContext = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier);
        val signedTransaction = txBuilderContext.buildAndSign(txBuilder, sender.getTxSigner());

        return signedTransaction.serialize();
    }

}
