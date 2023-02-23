package org.cardanofoundation.hydra.client;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Party;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.*;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.util.Optional;

@Slf4j
public class ResponseDeserializer {

    public Optional<QueryResponse> deserialize(String json) {
        val raw = MoreJson.read(json);
        val tagString = raw.get("tag").asText();
        val tag = Tag.find(tagString);

        val answer = tag.flatMap(t -> {
            if (t == Tag.ReadyToCommit) {
                val utxoNode = raw.get("parties");
                val parties = MoreJson.<Party>convertList(utxoNode);

                val command = new ReadyToCommitResponse(parties);

                return Optional.of(command);
            }
            if (t == Tag.HeadIsOpen) {
                val utxoNode = raw.get("utxo");
                val utxoMap = MoreJson.<UTXO>convertStringMap(utxoNode);

                val command = new HeadIsOpenResponse(t, utxoMap);

                return Optional.of(command);
            }
            if (t == Tag.CommandFailed) {
                val clientInputNode = raw.get("clientInput");
                val command = new CommandFailedResponse(clientInputNode);

                return Optional.of(command);
            }
            if (t == Tag.Committed) {
                val party = MoreJson.convert(raw.get("party"), Party.class);
                val utxo = MoreJson.<UTXO>convertStringMap(raw.get("utxo"));
                val command = new CommittedResponse(party, utxo);

                return Optional.of(command);
            }
            if (t == Tag.PostTxOnChainFailed) {
                return Optional.of(new PostTxOnChainFailedResponse());
            }
            if (t == Tag.PeerConnected) {
                val peer = raw.get("peer").asText();
                return Optional.of(new PeerConnectedResponse(peer));
            }
            if (t == Tag.Greetings) {
                val party = MoreJson.convert(raw.get("me"), Party.class);
                return Optional.of(new Greetings(party));
            }
            if (t == Tag.RolledBack) {
                return Optional.of(new Rolledback());
            }

            log.warn("Unable to process json:" + json);

            return Optional.empty();

        });

        if (answer.isEmpty()) {
            log.error("response not handled properly, probably handler is missing:{}", json);
        }

        return answer;
    }

}
