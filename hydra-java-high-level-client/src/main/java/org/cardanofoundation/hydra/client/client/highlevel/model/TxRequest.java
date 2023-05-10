package org.cardanofoundation.hydra.client.client.highlevel.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
@ToString
public
class TxRequest implements Request {

    private final String txId;
    private final ConfirmationType confirmationType;

    @Override
    public String key() {
        return String.format("%s:%s:%s", getClass().getName(), confirmationType.getClass(), txId);
    }

}
