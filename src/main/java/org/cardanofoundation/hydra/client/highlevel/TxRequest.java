package org.cardanofoundation.hydra.client.highlevel;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
@ToString
class TxRequest implements Request {

    private final String txId;
    private final ConfirmationType confirmationType;

    @Override
    public String key() {
        return String.format("%s:%s:%s", getClass().getName(), confirmationType.getClass(), txId);
    }

}
