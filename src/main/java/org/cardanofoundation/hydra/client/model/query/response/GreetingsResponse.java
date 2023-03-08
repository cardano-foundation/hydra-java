package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.LocalDateTime;

// A friendly welcome message which tells a client something about the node. Currently used for knowing what Party the server embodies. This message produced whenever the hydra-node starts and clients should take consequence of seeing this. For example, we can assume no peers connected when we see 'Greetings'.
@Getter
@ToString(callSuper = true)
public class GreetingsResponse extends Response {

    private final Party me;
    private final LocalDateTime timestamp;

    public GreetingsResponse(Party party, int seq, LocalDateTime timestamp) {
        super(Tag.Greetings, seq);
        this.me = party;
        this.timestamp = timestamp;
    }

    public static GreetingsResponse create(JsonNode raw) {
        val party = MoreJson.convert(raw.get("me"), Party.class);
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);

        return new GreetingsResponse(party, seq, timestamp);
    }

}
