package org.cardanofoundation.hydra.core.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.HydraState;
import org.cardanofoundation.hydra.core.model.Party;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.store.UTxOStore;
import org.cardanofoundation.hydra.core.utils.MoreJson;

import java.time.LocalDateTime;
import java.util.Map;

// A friendly welcome message which tells a client something about the node. Currently used for knowing what Party the server embodies. This message produced whenever the hydra-node starts and clients should take consequence of seeing this. For example, we can assume no peers connected when we see 'Greetings'.
@Getter
@ToString(callSuper = true)
public class GreetingsResponse extends Response {

    private final Party me;

    private final LocalDateTime timestamp;

    private final HydraState headStatus;

    private final Map<String, UTXO> snapshotUtxo;

    public GreetingsResponse(Party party,
                             int seq,
                             LocalDateTime timestamp,
                             HydraState headStatus,
                             Map<String, UTXO> snapshotUtxo) {
        super(Tag.Greetings, seq);
        this.me = party;
        this.timestamp = timestamp;
        this.headStatus = headStatus;
        this.snapshotUtxo = snapshotUtxo;
    }

    public static GreetingsResponse create(UTxOStore uTxOStore, JsonNode raw) {
        val party = MoreJson.convert(raw.get("me"), Party.class);
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);
        val headStatus = MoreJson.convert(raw.get("headStatus"), HydraState.class);
        if (raw.has("snapshotUtxo")) {
            val utxo = MoreJson.convertUTxOMap(raw.get("snapshotUtxo"));

            uTxOStore.storeLatestUtxO(utxo);

            return new GreetingsResponse(party, seq, timestamp, headStatus, utxo);
        }

        return new GreetingsResponse(party, seq, timestamp, headStatus, Map.of());
    }

}
