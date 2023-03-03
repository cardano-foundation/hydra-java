package org.cardanofoundation.hydra.client.model.query.request;

import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.Tag;
import org.stringtemplate.v4.ST;

@ToString
public class CloseHeadRequest extends Request {

    public CloseHeadRequest() {
        super(Tag.Close);
    }

    @Override
    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\" }");
        template.add("tag", tag);

        return template.render();
    }

}
