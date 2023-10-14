package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import org.cardanofoundation.hydra.core.model.http.HydraProtocolParameters;

import static org.cardanofoundation.hydra.cardano.client.lib.utils.MoreObjects.toBigDecimal;
import static org.cardanofoundation.hydra.cardano.client.lib.utils.MoreObjects.toStringNullSafe;

public class HydraNodeProtocolParametersAdapter implements ProtocolParamsSupplier {

    private final HydraProtocolParameters hydraProtocolParameters;

    public HydraNodeProtocolParametersAdapter(HydraProtocolParameters hydraProtocolParameters) {
        this.hydraProtocolParameters = hydraProtocolParameters;
    }

    public ProtocolParams getProtocolParams() {
        return ProtocolParams.builder()
                .minFeeA(hydraProtocolParameters.getMinFeeA())
                .minFeeB(hydraProtocolParameters.getMinFeeB())
                .maxBlockSize(hydraProtocolParameters.getMaxBlockBodySize())
                .maxBlockHeaderSize(hydraProtocolParameters.getMaxBlockHeaderSize())
                .maxTxSize(hydraProtocolParameters.getMaxTxSize())
                .keyDeposit(toStringNullSafe(hydraProtocolParameters.getKeyDeposit()))
                .poolDeposit(toStringNullSafe(hydraProtocolParameters.getPoolDeposit()))
                .eMax(hydraProtocolParameters.getEMax())
                .nOpt(hydraProtocolParameters.getNOpt())
                .a0(hydraProtocolParameters.getA0())
                .rho(hydraProtocolParameters.getRho())
                .tau(hydraProtocolParameters.getTau())
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

}
