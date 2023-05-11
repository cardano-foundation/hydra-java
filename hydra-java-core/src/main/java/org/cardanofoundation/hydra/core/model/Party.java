package org.cardanofoundation.hydra.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Party {

    private String vkey;

    @Override
    public String toString() {
        return "Party{" +
                "vkey='" + vkey + '\'' +
                '}';
    }

}
