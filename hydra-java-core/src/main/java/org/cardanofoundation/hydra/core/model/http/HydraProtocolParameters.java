package org.cardanofoundation.hydra.core.model.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class HydraProtocolParameters {

    @JsonProperty("minFeeA")
    private Integer minFeeA;

    @JsonProperty("minFeeB")
    private Integer minFeeB;

    @JsonProperty("maxBlockBodySize")
    private Integer maxBlockBodySize;

    @JsonProperty("maxTxSize")
    private Integer maxTxSize;

    @JsonProperty("maxBlockHeaderSize")
    private Integer maxBlockHeaderSize;

    @JsonProperty("keyDeposit")
    private Long keyDeposit;

    @JsonProperty("poolDeposit")
    private Long poolDeposit;

    @JsonProperty("eMax")
    private Integer eMax;

    @JsonProperty("nOpt")
    private Integer nOpt;

    @JsonProperty("a0")
    private BigDecimal a0;

    @JsonProperty("rho")
    private BigDecimal rho;

    @JsonProperty("tau")
    private BigDecimal tau;

    @JsonProperty("protocolVersion")
    private ProtocolVersion protocolVersion;

    @JsonProperty("minPoolCost")
    private Integer minPoolCost;

    @JsonProperty("costmdls")
    private CostModels costmdls;

    @JsonProperty("prices")
    private Prices prices;

    @JsonProperty("maxTxExUnits")
    private ExUnits maxTxExUnits;

    @JsonProperty("maxBlockExUnits")
    private ExUnits maxBlockExUnits;

    @JsonProperty("maxValSize")
    private Integer maxValSize;

    @JsonProperty("collateralPercentage")
    private Integer collateralPercentage;

    @JsonProperty("maxCollateralInputs")
    private Integer maxCollateralInputs;

    @JsonProperty("coinsPerUTxOByte")
    private Integer coinsPerUTxOByte;

    @Data
    public static class ProtocolVersion {
        @JsonProperty("major")
        private Integer major;

        @JsonProperty("minor")
        private Integer minor;
    }

    @Data
    public static class CostModels {
    }

    @Data
    public static class Prices {
        @JsonProperty("prMem")
        private Integer prMem;

        @JsonProperty("prSteps")
        private Integer prSteps;
    }

    @Data
    public static class ExUnits {
        @JsonProperty("exUnitsMem")
        private Long exUnitsMem;

        @JsonProperty("exUnitsSteps")
        private Long exUnitsSteps;
    }

}
