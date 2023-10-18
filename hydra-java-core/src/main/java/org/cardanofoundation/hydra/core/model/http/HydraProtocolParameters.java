package org.cardanofoundation.hydra.core.model.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class HydraProtocolParameters {

    @JsonProperty("minFeeA")
    private int minFeeA;

    @JsonProperty("minFeeB")
    private int minFeeB;

    @JsonProperty("maxBlockBodySize")
    private int maxBlockBodySize;

    @JsonProperty("maxTxSize")
    private int maxTxSize;

    @JsonProperty("maxBlockHeaderSize")
    private int maxBlockHeaderSize;

    @JsonProperty("keyDeposit")
    private int keyDeposit;

    @JsonProperty("poolDeposit")
    private int poolDeposit;

    @JsonProperty("eMax")
    private int eMax;

    @JsonProperty("nOpt")
    private int nOpt;

    @JsonProperty("a0")
    private double a0;

    @JsonProperty("rho")
    private double rho;

    @JsonProperty("tau")
    private double tau;

    @JsonProperty("protocolVersion")
    private ProtocolVersion protocolVersion;

    @JsonProperty("minPoolCost")
    private int minPoolCost;

    @JsonProperty("costmdls")
    private CostModels costModels;

    @JsonProperty("prices")
    private Prices prices;

    @JsonProperty("maxTxExUnits")
    private ExUnits maxTxExUnits;

    @JsonProperty("maxBlockExUnits")
    private ExUnits maxBlockExUnits;

    @JsonProperty("maxValSize")
    private int maxValSize;

    @JsonProperty("collateralPercentage")
    private int collateralPercentage;

    @JsonProperty("maxCollateralInputs")
    private int maxCollateralInputs;

    @JsonProperty("coinsPerUTxOByte")
    private int coinsPerUTxOByte;

    @Data
    public static class ProtocolVersion {
        @JsonProperty("major")
        private int major;

        @JsonProperty("minor")
        private int minor;

    }

    @Data
    public static class CostModels {
        @JsonProperty("PlutusV1")
        private long[] plutusV1;

        @JsonProperty("PlutusV2")
        private long[] plutusV2;

    }

    @Data
    public static class Prices {

        @JsonProperty("prMem")
        private int prMem;

        @JsonProperty("prSteps")
        private int prSteps;

    }

    @Data

    public static class ExUnits {
        @JsonProperty("exUnitsMem")
        private long exUnitsMem;

        @JsonProperty("exUnitsSteps")
        private long exUnitsSteps;

    }

}
