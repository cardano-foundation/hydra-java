package org.cardanofoundation.hydra.cardano.client.lib.transaction;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import lombok.val;
import org.cardanofoundation.hydra.cardano.client.lib.wallet.CardanoOperator;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;

public class SimpleTransactionCreator {

    private final UtxoSupplier utxoSupplier;

    private final ProtocolParamsSupplier protocolParamsSupplier;

    public SimpleTransactionCreator(UtxoSupplier utxoSupplier,
                                    ProtocolParamsSupplier protocolParamsSupplier) {
        this.utxoSupplier = utxoSupplier;
        this.protocolParamsSupplier = protocolParamsSupplier;
    }

    public byte[] simpleTransfer(CardanoOperator sender,
                                 CardanoOperator receiver,
                                 int adaAmount) throws CborSerializationException {
        var senderAddress = sender.getAddress();
        var receiverAddress = receiver.getAddress();

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
