package org.cardanofoundation.hydra.client.model.query.request;

import lombok.val;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.Tag;
import org.stringtemplate.v4.ST;

public class InitRequest extends Request {

    private static final Tag QUERY_TYPE = Tag.Init;

    public InitRequest() {
        super(QUERY_TYPE);
    }

    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\" }");
        template.add("tag", tag);

        return template.render();
    }

    @Override
    public String toString() {
        return "Init{" +
                "tag=" + tag +
                '}';
    }

}
