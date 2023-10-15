package org.cardanofoundation.hydra.reactor;

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
import static org.cardanofoundation.hydra.core.utils.MoreJson.readValue;
import static org.cardanofoundation.hydra.core.utils.MoreJson.serialise;

public class HydraReactiveWebClient {

    private static final Duration DEF_TIMEOUT = Duration.ofMinutes(1);

    private final HttpClient httpClient;

    private final String baseUrl;
    private final Duration timeout;

    public HydraReactiveWebClient(HttpClient httpClient,
                                  String baseUrl,
                                  Duration timeout) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    public HydraReactiveWebClient(HttpClient httpClient,
                                  String baseUrl) {
        this(httpClient, baseUrl, DEF_TIMEOUT);
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
                                .thenApply(responseBody -> readValue(responseBody, HydraProtocolParameters.class))
                ).timeout(timeout)
                .onErrorMap(this::handleError);
    }

    public Mono<HeadCommitResponse> commitRequest(Map<String, UTXO> commitDataMap) {
        var serialisedJson = serialise(commitDataMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(commitUrl())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serialisedJson))
                .build();

        return Mono.fromFuture(() ->
                        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                                .thenApply(HttpResponse::body)
                                .thenApply(responseBody -> readValue(responseBody, HeadCommitResponse.class))
                ).timeout(timeout)
                .onErrorMap(this::handleError);
    }

    private Throwable handleError(Throwable t) {
        return new HydraException("An error occurred during the public class HydraReactiveWebClient {\n request", t);
    }

}
