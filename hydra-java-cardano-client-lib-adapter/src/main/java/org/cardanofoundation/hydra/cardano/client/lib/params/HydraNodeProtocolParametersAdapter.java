package org.cardanofoundation.hydra.cardano.client.lib.params;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import org.cardanofoundation.hydra.core.model.http.HydraProtocolParameters;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cardanofoundation.hydra.core.utils.MoreObjects.toBigDecimal;
import static org.cardanofoundation.hydra.core.utils.MoreObjects.toStringNullSafe;

/**
 * This class is used to convert the HydraProtocolParameters to ProtocolParams, following
 * format found in Hydra-Node 1.35.x
 */
public class HydraNodeProtocolParametersAdapter implements ProtocolParamsSupplier {

    private final HydraProtocolParameters hydraProtocolParameters;

    public HydraNodeProtocolParametersAdapter(HydraProtocolParameters hydraProtocolParameters) {
        this.hydraProtocolParameters = hydraProtocolParameters;
    }

    public ProtocolParams getProtocolParams() {
        var costModels = hydraProtocolParameters.getCostModels();

        return ProtocolParams.builder()
                .minFeeA(hydraProtocolParameters.getMinFeeA())
                .minFeeB(hydraProtocolParameters.getMinFeeB())

                .costModels(
                        Map.of(
                        "PlutusV1",
                                newCostModelToCCLCostModel(costModels.getPlutusV1()),
                        "PlutusV2",
                                newCostModelToCCLCostModel(costModels.getPlutusV2())
                        ))

                .maxBlockSize(hydraProtocolParameters.getMaxBlockBodySize())
                .maxBlockHeaderSize(hydraProtocolParameters.getMaxBlockHeaderSize())
                .maxTxSize(hydraProtocolParameters.getMaxTxSize())
                .keyDeposit(toStringNullSafe(hydraProtocolParameters.getKeyDeposit()))
                .poolDeposit(toStringNullSafe(hydraProtocolParameters.getPoolDeposit()))
                .eMax(hydraProtocolParameters.getEMax())
                .nOpt(hydraProtocolParameters.getNOpt())
                .a0(BigDecimal.valueOf(hydraProtocolParameters.getA0()))
                .rho(BigDecimal.valueOf(hydraProtocolParameters.getRho()))
                .tau(BigDecimal.valueOf(hydraProtocolParameters.getTau()))
                .protocolMajorVer(hydraProtocolParameters.getProtocolVersion().getMajor())
                .protocolMinorVer(hydraProtocolParameters.getProtocolVersion().getMinor())
                .minPoolCost(toStringNullSafe(hydraProtocolParameters.getMinPoolCost()))
                .priceStep(toBigDecimal(hydraProtocolParameters.getPrices().getPrSteps()))
                .priceMem(toBigDecimal(hydraProtocolParameters.getPrices().getPrMem()))
                .maxTxExMem(toStringNullSafe(hydraProtocolParameters.getMaxTxExUnits().getExUnitsMem()))
                .maxTxExSteps(toStringNullSafe(hydraProtocolParameters.getMaxTxExUnits().getExUnitsSteps()))
                .maxValSize(toStringNullSafe(hydraProtocolParameters.getMaxValSize()))
                .maxBlockExMem(toStringNullSafe(hydraProtocolParameters.getMaxBlockExUnits().getExUnitsMem()))
                .maxBlockExSteps(toStringNullSafe(hydraProtocolParameters.getMaxBlockExUnits().getExUnitsSteps()))
                .collateralPercent(toBigDecimal(hydraProtocolParameters.getCollateralPercentage()))
                .maxCollateralInputs(hydraProtocolParameters.getMaxCollateralInputs())
                .coinsPerUtxoSize(toStringNullSafe(hydraProtocolParameters.getCoinsPerUTxOByte()))
                .build();
    }

    private Map<String, Long> newCostModelToCCLCostModel(long[] newCostModel) {
        Map<String, Long> costModel = new LinkedHashMap<>();

        int index = 0;
        for (Long cost : newCostModel) {
            costModel.put(String.format("%03d", index++), cost);
        }

        return costModel;
    }

}
