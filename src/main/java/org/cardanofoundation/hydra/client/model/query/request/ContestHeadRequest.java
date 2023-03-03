package org.cardanofoundation.hydra.client.model.query.request;

import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.Tag;
import org.stringtemplate.v4.ST;

@Getter
@ToString(callSuper = true)
public class ContestHeadRequest extends Request {

    public ContestHeadRequest() {
        super(Tag.Contest);
    }

    @Override
    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\" }");
        template.add("tag", tag);

        return template.render();
    }

}
