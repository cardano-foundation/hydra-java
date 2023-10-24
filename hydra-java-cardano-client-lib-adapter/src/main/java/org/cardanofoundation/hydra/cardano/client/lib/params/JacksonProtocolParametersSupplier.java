package org.cardanofoundation.hydra.cardano.client.lib.params;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Getter
@Setter
/**
 * This class is used to convert the HydraProtocolParameters to ProtocolParams, following legacy
 * protocol-parameters format found in node 1.35.x
 */
@Deprecated
public class JacksonProtocolParametersSupplier implements ProtocolParamsSupplier {

    private int minFeeA = 44;
    private int minFeeB = 155381;

    private final JsonNode protoParamsJson;

    public JacksonProtocolParametersSupplier(JsonNode protoParamsJson) {
        this.protoParamsJson = protoParamsJson;
    }

    @Override
    public ProtocolParams getProtocolParams() {
        ProtocolParams protocolParams = new ProtocolParams();
        protocolParams.setCollateralPercent(new BigDecimal(protoParamsJson.get("collateralPercentage").asText()));
        String utxoCostPerByte = protoParamsJson.get("utxoCostPerByte").asText();
        String utxoCostPerWord = protoParamsJson.get("utxoCostPerWord").asText();

        protocolParams.setCoinsPerUtxoSize(utxoCostPerByte);
        protocolParams.setCoinsPerUtxoWord(utxoCostPerWord);
        protocolParams.setMinFeeA(minFeeA);
        protocolParams.setMinFeeB(minFeeB);
        protocolParams.setPriceMem(new BigDecimal(protoParamsJson.get("executionUnitPrices").get("priceMemory").asText()));
        protocolParams.setPriceStep(new BigDecimal(protoParamsJson.get("executionUnitPrices").get("priceSteps").asText()));
        protocolParams.setMaxTxExMem(protoParamsJson.get("maxTxExecutionUnits").get("memory").asText());
        protocolParams.setMaxTxExSteps(protoParamsJson.get("maxTxExecutionUnits").get("steps").asText());

        Map<String, Long> costModel1 = costModelFor("PlutusScriptV1");
        Map<String, Long> costModel2 = costModelFor("PlutusScriptV2");
        protocolParams.setCostModels(Map.of("PlutusV1", costModel1, "PlutusV2", costModel2));

        return protocolParams;
    }

    private Map<String, Long> costModelFor(String lang) {
        JsonNode plutusV2CostJson = protoParamsJson.get("costModels").get(lang);
        Iterator<String> opsIter = plutusV2CostJson.fieldNames();

        Map<String, Long> costModel = new HashMap<>();

        while (opsIter.hasNext()) {
            String op = opsIter.next();
            costModel.put(op, plutusV2CostJson.get(op).asLong());
        }

        return costModel;
    }

}
