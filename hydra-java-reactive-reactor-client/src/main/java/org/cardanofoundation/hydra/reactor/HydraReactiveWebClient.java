package org.cardanofoundation.hydra.reactor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.http.HeadCommitResponse;
import org.cardanofoundation.hydra.core.model.http.HydraProtocolParameters;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static java.lang.String.format;

public class HydraReactiveWebClient {

    private static final Duration DEF_TIMEOUT = Duration.ofMinutes(1);

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final String baseUrl;
    private final Duration timeout;

    public HydraReactiveWebClient(HttpClient httpClient,
                                  ObjectMapper objectMapper,
                                  String baseUrl,
                                  Duration timeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    public HydraReactiveWebClient(HttpClient httpClient,
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


    public Mono<HydraProtocolParameters> fetchProtocolParameters() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(protocolParametersUrl())
                .GET()
                .build();

        return Mono.fromFuture(() ->
                        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                .thenApply(HttpResponse::body)
                                .thenApply(responseBody -> {
                                    try {
                                        return objectMapper.readValue(responseBody, HydraProtocolParameters.class);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                ).timeout(timeout)
                .onErrorMap(this::handleError);
    }

    public Mono<HeadCommitResponse> commitRequest(Map<String, UTXO> commitDataMap) {
        try {
            var serialisedJson = objectMapper.writeValueAsString(commitDataMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(commitUrl())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(serialisedJson))
                    .build();

            return Mono.fromFuture(() ->
                            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                    .thenApply(HttpResponse::body)
                                    .thenApply(responseBody -> {
                                        try {
                                            return objectMapper.readValue(responseBody, HeadCommitResponse.class);
                                        } catch (JsonProcessingException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                    ).timeout(timeout)
                    .onErrorMap(this::handleError);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    private Throwable handleError(Throwable t) {
        return new HydraException("An error occurred during the public class HydraReactiveWebClient {\n request", t);
    }

}
