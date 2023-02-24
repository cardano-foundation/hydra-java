package org.cardanofoundation.hydra.client.model.query.request;

import lombok.val;
import org.cardanofoundation.hydra.client.model.Request;
import org.cardanofoundation.hydra.client.model.Tag;
import org.stringtemplate.v4.ST;

public class InitRequest extends Request {

    private static final Tag QUERY_TYPE = Tag.Init;

    private int contestationPeriod = 100;

    public InitRequest(int contestationPeriod) {
        super(QUERY_TYPE);
        this.contestationPeriod = contestationPeriod;
    }

    public String getRequestBody() {
        val template = new ST("{ \"tag\": \"<tag>\", \"contestationPeriod\": <contestation_period>}");
        template.add("tag", tag);
        template.add("contestation_period", contestationPeriod);

        return template.render();
    }

}
