package org.cardanofoundation.hydra.client.client.helpers;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import lombok.val;
import org.cardanofoundation.hydra.cardano.client.lib.HydraOperator;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;

public class HydraTransactionGenerator {

    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;

    public HydraTransactionGenerator(UtxoSupplier utxoSupplier,
                                     ProtocolParamsSupplier protocolParamsSupplier) {
        this.utxoSupplier = utxoSupplier;
        this.protocolParamsSupplier = protocolParamsSupplier;
    }

    public byte[] simpleTransfer(HydraOperator sender, HydraOperator receiver, int adaAmount) throws CborSerializationException {
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
