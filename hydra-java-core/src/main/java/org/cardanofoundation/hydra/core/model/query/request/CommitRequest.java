package org.cardanofoundation.hydra.core.model.query.request;

import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Request;
import org.cardanofoundation.hydra.core.model.Tag;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.utils.MoreJson;
import org.stringtemplate.v4.ST;

import java.util.HashMap;
import java.util.Map;

@ToString
public class CommitRequest extends Request {

    private Map<String, UTXO> utxo;

    public CommitRequest() {
        super(Tag.Commit);
        utxo = new HashMap<>();
    }

    public void addUTXO(String utxoId, UTXO utxo) {
        this.utxo.put(utxoId, utxo);
    }

    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\", \"utxo\": <utxo>}");
        template.add("tag", tag);

        if (utxo.isEmpty()) {
            template.add("utxo", "{}");
        } else {
            template.add("utxo", MoreJson.serialise(utxo));
        }

        return template.render();
    }

}
