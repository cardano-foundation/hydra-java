package org.cardanofoundation.hydra.core.model.query.request;

import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.core.model.Request;
import org.cardanofoundation.hydra.core.model.Tag;
import org.stringtemplate.v4.ST;

@Getter
@ToString(callSuper = true)
public class FanoutRequest extends Request {

    public FanoutRequest() {
        super(Tag.Fanout);
    }

    @Override
    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\" }");
        template.add("tag", tag);

        return template.render();
    }

}
