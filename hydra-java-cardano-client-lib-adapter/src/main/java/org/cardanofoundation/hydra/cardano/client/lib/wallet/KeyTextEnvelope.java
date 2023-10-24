package org.cardanofoundation.hydra.cardano.client.lib.wallet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cardanofoundation.hydra.core.utils.HexUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyTextEnvelope {

    @JsonIgnore
    private KeyTextEnvelopeType type;

    @JsonIgnore
    private byte[] cbor;

    @JsonProperty("type")
    final public String getTypeValue() {
        return this.type.getType();
    }

    @JsonProperty("type")
    final public void setTypeValue(final String typeValue) {
        this.type = KeyTextEnvelopeType.fromTypeValue(typeValue);
    }

    @JsonIgnore
    @JsonProperty("description")
    public void setDescription(final String description) {
    }

    @JsonProperty("description")
    final public String getDescription() {
        return this.type.getDescription();
    }

    @JsonProperty("cborHex")
    final public String getCborHex() {
        return HexUtils.encodeHexString(this.cbor);
    }

    @JsonProperty("cborHex")
    final public void setCborHex(final String value) {
        this.cbor = HexUtils.decodeHexString(value);
    }

}