package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.cardanofoundation.hydra.client.model.Tag;

@Getter
//   The contestation period has passed and the head can now be finalized by
//   a fanout transaction.
public class ReadyToFanoutResponse extends Response {

    public ReadyToFanoutResponse() {
        super(Tag.ReadyToFanout);
    }

    public static ReadyToFanoutResponse create(JsonNode raw) {
        return new ReadyToFanoutResponse();
    }

    @Override
    public String toString() {
        return "ReadyToFanout{" +
                "tag=" + tag +
                '}';
    }
}

//{
//        "tag": "ReadyToFanout"
//}