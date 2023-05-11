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
import org.cardanofoundation.hydra.core.store.UTxOStore;
import org.cardanofoundation.hydra.core.utils.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter.toPlutusData;

public class SnapshotUTxOSupplier implements UtxoSupplier {

    private final UTxOStore utxoStore;

    public SnapshotUTxOSupplier(UTxOStore utxoStore) {
        this.utxoStore = utxoStore;
    }

    @Override
    public List<Utxo> getPage(String address, Integer nrOfItems, Integer page, OrderEnum order) {
        var snapshot = utxoStore.getLatestUTxO();
        if (snapshot.isEmpty()) {
            return Collections.emptyList();
        }
        // no paging support in this supplier
        if (page >= 1) {
            return List.of();
        }

        return snapshot.entrySet()
                .stream()
                .filter(utxoEntry -> utxoEntry.getValue().getAddress().equals(address))
                .map(utxoEntry -> new Tuple<>(StringUtils.split(utxoEntry.getKey(), "#"), utxoEntry.getValue()))
                .peek(t -> System.out.println(Arrays.asList(t._1)))
                .peek(t -> System.out.println(t._2))
                .map(tuple -> {
                    String utxoId = tuple._1[0];
                    int outputIndex = Integer.parseInt(tuple._1[1]);
                    UTXO utxo = tuple._2;

                    return Utxo.builder()
                            .txHash(utxoId)
                            .outputIndex(outputIndex)
                            .address(address)
                            .amount(utxo.getValue().entrySet()
                                    .stream()
                                    .map(entry -> new Amount(entry.getKey(), entry.getValue()))
                                    .collect(Collectors.toList()))
                            .dataHash(utxo.getDatumhash())
                            .inlineDatum(convertInlineDatum(utxo.getInlineDatum()))
                            .referenceScriptHash(utxo.getReferenceScript())
                            .build();

                })
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
