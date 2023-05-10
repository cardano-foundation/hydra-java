package org.cardanofoundation.hydra.client.client.utils;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import lombok.SneakyThrows;
import lombok.val;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;

public class SerialisedTransactionsSupplier {

    private final UtxoSupplier utxoSupplier;
    private final ProtocolParamsSupplier protocolParamsSupplier;
    private final JacksonClasspathSecretKeyAccountSupplier sender;
    private final JacksonClasspathSecretKeyAccountSupplier receiver;

    public SerialisedTransactionsSupplier(UtxoSupplier utxoSupplier,
                                          ProtocolParamsSupplier protocolParamsSupplier,
                                          JacksonClasspathSecretKeyAccountSupplier sender,
                                          JacksonClasspathSecretKeyAccountSupplier receiver) {
        this.utxoSupplier = utxoSupplier;
        this.protocolParamsSupplier = protocolParamsSupplier;
        this.sender = sender;
        this.receiver = receiver;
    }

    @SneakyThrows
    public byte[] sendAdaTransaction(int adaAmount) {
        var output = Output.builder()
                .address(receiver.getOperatorAddress())
                .assetName(LOVELACE)
                .qty(adaToLovelace(adaAmount))
                .build();

        var txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(sender.getOperatorAddress(), sender.getOperatorAddress()))
                .andThen(balanceTx(sender.getOperatorAddress(), 1));

        var a = utxoSupplier.getAll(sender.getOperatorAddress());

        val txBuilderContext = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier);
        val signedTransaction = txBuilderContext.buildAndSign(txBuilder, sender.getTxSigner());

        return signedTransaction.serialize();
    }

}
