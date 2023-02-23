package org.cardanofoundation.hydra.client.model.query.request;

import lombok.val;
import org.cardanofoundation.hydra.client.model.query.request.base.SubmitRequest;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.stringtemplate.v4.ST;

public class NewTxRequest extends SubmitRequest {

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

}
