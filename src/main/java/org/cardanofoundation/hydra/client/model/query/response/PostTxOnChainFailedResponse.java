package org.cardanofoundation.hydra.client.model.query.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;
import lombok.val;
import org.cardanofoundation.hydra.client.model.PostChainTx;
import org.cardanofoundation.hydra.client.model.Tag;
import org.cardanofoundation.hydra.client.internal.utils.MoreJson;

import java.time.LocalDateTime;


// Something wrong happened when trying to post a transaction on-chain. Provides information about what kind of transaction was tentatively posted, and the reason for failure.
@Getter
@ToString(callSuper = true)
public class PostTxOnChainFailedResponse extends Response implements FailureResponse {

    private final LocalDateTime timestamp;
    private final PostChainTx postChainTx;

    public PostTxOnChainFailedResponse(int seq, LocalDateTime timestamp, PostChainTx postChainTx) {
        super(Tag.PostTxOnChainFailed, seq, true);
        this.timestamp = timestamp;
        this.postChainTx = postChainTx;
    }

    public static PostTxOnChainFailedResponse create(JsonNode raw) {
        val seq = raw.get("seq").asInt();
        val timestamp = MoreJson.convert(raw.get("timestamp"), LocalDateTime.class);
        val postChainTx = MoreJson.convert(raw.get("postChainTx"), PostChainTx.class);

        return new PostTxOnChainFailedResponse(seq, timestamp, postChainTx);
    }

    @Override
    public boolean isLowLevelFailure() {
        return postChainTx.getTag() == Tag.CollectComTx;
    }

}
