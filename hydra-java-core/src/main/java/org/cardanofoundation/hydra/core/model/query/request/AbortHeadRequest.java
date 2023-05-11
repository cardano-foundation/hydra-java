package org.cardanofoundation.hydra.core.model.query.request;

import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Request;
import org.cardanofoundation.hydra.core.model.Tag;
import org.stringtemplate.v4.ST;

@ToString
public class AbortHeadRequest extends Request {

    public AbortHeadRequest() {
        super(Tag.Abort);
    }

    @Override
    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\" }");
        template.add("tag", tag);

        return template.render();
    }

}
