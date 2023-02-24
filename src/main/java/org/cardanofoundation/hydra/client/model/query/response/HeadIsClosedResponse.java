package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import org.cardanofoundation.hydra.client.model.query.request.base.Tag;
import org.cardanofoundation.hydra.client.model.query.response.base.QueryResponse;
import org.cardanofoundation.hydra.client.util.MoreJson;

import java.time.ZonedDateTime;

@Getter
public class HeadIsClosedResponse extends QueryResponse {

    private int snapshotNumber;

    private ZonedDateTime contestationDeadline;

    public HeadIsClosedResponse(int snapshotNumber, ZonedDateTime contestationDeadline) {
        super(Tag.HeadIsClosed);
        this.snapshotNumber = snapshotNumber;
        this.contestationDeadline = contestationDeadline;
    }

    public static HeadIsClosedResponse create(JsonNode raw) {
        val snapshotNumber = raw.get("snapshotNumber").asInt();
        val contestationDeadline = MoreJson.convert(raw.get("contestationDeadline"), ZonedDateTime.class);

        return new HeadIsClosedResponse(snapshotNumber, contestationDeadline);
    }

}

//{
//        "contestationDeadline": "2022-11-07T13:08:22Z",
//        "snapshotNumber": 1,
//        "tag": "HeadIsClosed"
//}