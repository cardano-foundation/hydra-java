package org.cardanofoundation.hydra.client.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Party {

    String vkey;

    @Override
    public String toString() {
        return "Party{" +
                "vkey='" + vkey + '\'' +
                '}';
    }

}
