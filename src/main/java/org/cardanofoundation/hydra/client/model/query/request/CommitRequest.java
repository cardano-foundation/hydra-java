package org.cardanofoundation.hydra.client.model.query.request;

import lombok.val;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.model.UTXO;
import org.cardanofoundation.hydra.client.util.MoreJson;
import org.stringtemplate.v4.ST;

import java.util.HashMap;
import java.util.Map;

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
        template.add("utxo", MoreJson.serialise(utxo));

        return template.render();
    }

    @Override
    public String toString() {
        return "Commit{" +
                "utxo=" + utxo +
                ", tag=" + tag +
                '}';
    }

}

// {
//     "tag":"Commit",
//     "utxo":{
//        "ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0":{
//           "address":"addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3",
//           "value":{
//              "lovelace": 1000000000
//           }
//        }
//     }
//  }