package org.cardanofoundation.hydra.client.highlevel;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode
@ToString
public class Commit implements Request, Response {

    public final static Commit INSTANCE = new Commit();

    @Override
    public String key() {
        return getClass().getName();
    }

}
