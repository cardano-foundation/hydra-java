package org.cardanofoundation.hydra.client.client.highlevel.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
@ToString
public class Init implements Request, Response {

    public final static Init INSTANCE = new Init();

    @Override
    public String key() {
        return getClass().getName();
    }

}
