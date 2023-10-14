package org.cardanofoundation.hydra.core.model.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class HeadCommitResponse {

    @JsonProperty("cborHex")
    private String cborHex;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type;

}
