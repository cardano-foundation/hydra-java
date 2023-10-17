package org.cardanofoundation.hydra.cardano.client.lib;

import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.cardanofoundation.hydra.core.utils.HexUtils.decodeHexString;

@RequiredArgsConstructor
@Slf4j
public class CardanoTxSubmissionClient {

    private final HttpClient httpClient;

    private final String cardanoSubmitApiUrl;

    public Result<String> submitTransaction(Transaction transaction) {
        try {
            return submitTransaction(transaction.serializeToHex());
        } catch (CborSerializationException e) {
            return Result.error("Cbor serialisation error, reason: " +  e.getMessage());
        }
    }

    public Result<String> submitTransaction(String cborHex) {
        return submitTransaction(decodeHexString(cborHex));
    }

    public Result<String> submitTransaction(byte[] txData) {
        var txTransactionSubmitPostRequest = HttpRequest.newBuilder()
                .uri(URI.create(cardanoSubmitApiUrl))
                .POST(HttpRequest.BodyPublishers.ofByteArray(txData))
                .header("Content-Type", "application/cbor")
                .build();
        try {
            var r = httpClient.send(txTransactionSubmitPostRequest, HttpResponse.BodyHandlers.ofString());

            if (r.statusCode() >= 200 && r.statusCode() < 300) {
                var txId = r.body();
                log.info("Submitted TxId: {}", txId);

                return Result.success(txId).withValue(txId);
            }

            return Result.error("Error submitting transaction, status code: " + r.statusCode() + ", body: " + r.body());
        } catch (IOException | InterruptedException e) {
            return Result.error("Error submitting transaction, reason:" + e.getMessage());
        }
    }

}
