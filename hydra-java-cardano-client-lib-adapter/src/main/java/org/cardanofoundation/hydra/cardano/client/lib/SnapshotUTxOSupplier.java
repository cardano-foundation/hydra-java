package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.util.Tuple;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.cardanofoundation.hydra.core.model.UTXO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter.toPlutusData;

public class SnapshotUTxOSupplier implements UtxoSupplier {

    private final Map<String, UTXO> snapshot;

    public SnapshotUTxOSupplier(final Map<String, UTXO> snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public List<Utxo> getPage(String address, Integer nrOfItems, Integer page, OrderEnum order) {
        if (snapshot.isEmpty()) {
            return Collections.emptyList();
        }
        // no paging support in hydra
        if (page >= 1) {
            throw new IllegalArgumentException("No pagination support!");
        }

        return snapshot.entrySet()
                .stream()
                .filter(utxoEntry -> utxoEntry.getValue().getAddress().equals(address))
                .map(utxoEntry -> new Tuple<>(utxoEntry.getKey().split("#"), utxoEntry.getValue()))
                .map(tuple -> Utxo.builder()
                        .txHash(tuple._1[0])
                        .outputIndex(Integer.parseInt(tuple._1[1]))
                        .address(address)
                        .amount(tuple._2.getValue().entrySet()
                                .stream()
                                .map(entry -> new Amount(entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList()))
                        .dataHash(tuple._2.getDatumhash())
                        .inlineDatum(convertInlineDatum(tuple._2.getInlineDatum()))
                        .referenceScriptHash(tuple._2.getReferenceScript())
                        .build())
                .limit(nrOfItems)
                .collect(Collectors.toList());
    }

    private String convertInlineDatum(JsonNode inlineDatum) {
        if (inlineDatum == null || inlineDatum instanceof NullNode)
            return null;

        try {
            PlutusData plutusData = toPlutusData(inlineDatum);
            return plutusData.serializeToHex();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to convert inlineDatum to PlutusData");
        }
    }

}
