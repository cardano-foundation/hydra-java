package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.util.Tuple;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.store.UTxOStore;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.client.plutus.spec.serializers.PlutusDataJsonConverter.toPlutusData;
import static org.cardanofoundation.hydra.core.utils.StringUtils.split;

public class SnapshotUTxOSupplier implements UtxoSupplier {

    private final UTxOStore utxoStore;

    public SnapshotUTxOSupplier(UTxOStore utxoStore) {
        this.utxoStore = utxoStore;
    }

    @Override
    public List<Utxo> getPage(String address,
                              Integer nrOfItems,
                              Integer page,
                              OrderEnum order) {
        var items = nrOfItems == null ? UtxoSupplier.DEFAULT_NR_OF_ITEMS_TO_FETCH : nrOfItems;
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
                .map(utxoEntry -> new Tuple<>(split(utxoEntry.getKey(), "#"), utxoEntry.getValue()))
                .map(tuple -> createUtxo(address, tuple))
                .limit(items)
                .toList();
    }

    @Override
    public Optional<Utxo> getTxOutput(String txHash, int outputIndex) {
        return getAll().stream()
                .filter(utxo -> utxo.getTxHash().equals(txHash) && utxo.getOutputIndex() == outputIndex)
                .findFirst();
    }

    public List<Utxo> getAll() {
        return utxoStore.getLatestUTxO().entrySet()
                .stream()
                .map(entry -> {
                    var utxo = entry.getValue();
                    var address = utxo.getAddress();
                    var txId = entry.getKey();

                    var tuple = new Tuple<>(split(txId, "#"), utxo);

                    return createUtxo(address, tuple);
                })
                .toList();
    }


    private static @Nullable String convertInlineDatum(@Nullable JsonNode inlineDatum) {
        if (inlineDatum == null || inlineDatum.isNull()) {
            return null;
        }

        try {
            var plutusData = toPlutusData(inlineDatum);

            return plutusData.serializeToHex();
        } catch (JsonProcessingException e) {
            throw new HydraException("Unable to convert inlineDatum to PlutusData");
        }
    }

    private static Utxo createUtxo(String address,
                                   Tuple<@Nullable String[], UTXO> tuple) {
        String txId = tuple._1[0];
        int outputIndex = Integer.parseInt(tuple._1[1]);
        UTXO utxo = tuple._2;

        return Utxo.builder()
                .txHash(txId)
                .outputIndex(outputIndex)
                .address(address)
                .amount(utxo.getValue().entrySet()
                        .stream()
                        .map(entry -> new Amount(entry.getKey(), entry.getValue()))
                        .toList())
                .dataHash(utxo.getDatumhash())
                .inlineDatum(convertInlineDatum(utxo.getInlineDatum()))
                .referenceScriptHash(utxo.getReferenceScript())
                .build();
    }

}
