package org.cardanofoundation.hydra.client.model.query.request;

import lombok.val;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.Tag;
import org.stringtemplate.v4.ST;

public class NewTxRequest extends Request {

    private final String transaction;

    public NewTxRequest(String transaction) {
        super(Tag.NewTx);
        this.transaction = transaction;
    }

    @Override
    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\", \"transaction\": <transaction>}");
        template.add("tag", tag);
        template.add("transaction", transaction);

        return template.render();
    }

    @Override
    public String toString() {
        return "NewTx{" +
                "transaction='" + transaction + '\'' +
                ", tag=" + tag +
                '}';
    }

}
