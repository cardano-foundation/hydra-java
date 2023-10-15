package org.cardanofoundation.hydra.core.model.query.request;

import lombok.val;
import org.cardanofoundation.hydra.core.model.Request;
import org.cardanofoundation.hydra.core.model.Tag;
import org.stringtemplate.v4.ST;

public class GetUTxORequest extends Request {

    /**
     * Create a new request.
     */
    public GetUTxORequest() {
        super(Tag.GetUTxO);
    }

    /**
     * The tag of the request.
     *
     * @return
     */
    @Override
    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\" }");
        template.add("tag", tag);

        return template.render();
    }

    @Override
    public String toString() {
        return "GetUTxO{" +
                "tag=" + tag +
                '}';
    }

}
