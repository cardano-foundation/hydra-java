package org.cardanofoundation.hydra.client.highlevel;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
@ToString
public class Connect implements Request, Response {

    public final static Connect INSTANCE = new Connect();

    @Override
    public String key() {
        return getClass().getName();
    }

}
