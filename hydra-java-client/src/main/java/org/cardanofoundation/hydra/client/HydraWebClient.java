package org.cardanofoundation.hydra.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.http.HeadCommitResponse;
import org.cardanofoundation.hydra.core.model.http.HydraProtocolParameters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

import static java.lang.String.format;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class HydraWebClient {

    private static final Duration DEF_TIMEOUT = Duration.ofMinutes(1);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final Duration timeout;

    public HydraWebClient(HttpClient httpClient,
                          ObjectMapper objectMapper,
                          String baseUrl,
                          Duration timeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    public HydraWebClient(HttpClient httpClient,
                          ObjectMapper objectMapper,
                          String baseUrl) {
        this(httpClient, objectMapper, baseUrl, DEF_TIMEOUT);
    }

    private URI commitUrl() {
        return URI.create(format("%s/commit", baseUrl));
    }

    private URI protocolParametersUrl() {
        return URI.create(format("%s/protocol-parameters", baseUrl));
    }

    public HydraProtocolParameters fetchProtocolParameters() throws HydraException {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(protocolParametersUrl())
                    .GET()
                    .header("Accept", "application/json")
                    .timeout(timeout)
                    .build();

            var response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), HydraProtocolParameters.class);
            }

            throw new IOException(format("Unable to read protocol parameters from the url: %s, statusCode: %d, reason: %s",
                    protocolParametersUrl(), response.statusCode(), response.body()));
        } catch (IOException | InterruptedException e) {
            throw new HydraException("Unable to read protocol parameters from the hydra head", e);
        }
    }

    public HeadCommitResponse commitRequest(Map<String, UTXO> commitDataMap) throws HydraException {
        try {
            var postBody = objectMapper.writeValueAsString(commitDataMap);

            var request = HttpRequest.newBuilder()
                    .uri(commitUrl())
                    .POST(ofString(postBody))
                    .header("Accept", "application/json")
                    .timeout(timeout)
                    .build();

            var response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), HeadCommitResponse.class);
            }

            throw new HydraException(format("Unable to commit to the head with the url: %s, statusCode: %d, reason: %s",
                    protocolParametersUrl(), response.statusCode(), response.body()));
        } catch (IOException | InterruptedException e) {
            throw new HydraException("Unable to commit utxos to the head", e);
        }
    }

    // TODO L1 transactions endpoint

}
