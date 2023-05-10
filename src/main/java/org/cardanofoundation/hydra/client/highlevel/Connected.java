package org.cardanofoundation.hydra.client.highlevel;


import lombok.Builder;
import org.cardanofoundation.hydra.client.model.query.response.GreetingsResponse;

@Builder
public class Connected {

    private GreetingsResponse greetings;

}
