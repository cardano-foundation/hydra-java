package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.cardanofoundation.hydra.core.HydraException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Simple implementation that sources protocol parameters from the classpath
 */
@Getter
@Setter
@Deprecated
// use HydraNodeProtocolParametersAdapter instead and read ProtocolParameters remotely from hydra node itself
public class JacksonClasspathProtocolParametersSupplier implements ProtocolParamsSupplier {

    private final JsonNode protoParamsJson;

    private int minFeeA = 44;
    private int minFeeB = 155381;

    public JacksonClasspathProtocolParametersSupplier(ObjectMapper objectMapper) {
        this(objectMapper, Optional.empty());
    }

    public JacksonClasspathProtocolParametersSupplier(ObjectMapper objectMapper, String classpathLink) {
        this(objectMapper, Optional.of(classpathLink));
    }

    private JacksonClasspathProtocolParametersSupplier(ObjectMapper objectMapper,
                                                       Optional<String> classpathLink) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpathLink.orElse("protocol-parameters.json"))) {
            protoParamsJson = objectMapper.readTree(is);
        } catch (IOException e) {
            throw new HydraException(e);
        }
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
