package org.cardanofoundation.hydra.client.model.query.request.base;

import org.cardanofoundation.hydra.client.model.base.MethodType;
import org.cardanofoundation.hydra.client.model.base.Request;

public abstract class QueryRequest extends Request {

    private static final MethodType METHOD_TYPE = MethodType.QUERY;

    protected QueryRequest(Tag tag) {
        super(tag);
    }

    @Override
    public String getMethodType() {
        return METHOD_TYPE.getValue();
    }

}
