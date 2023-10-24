package org.cardanofoundation.hydra.reactor;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.http.HeadCommitResponse;
import org.cardanofoundation.hydra.core.model.http.HydraProtocolParameters;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.net.http.HttpResponse.BodyHandlers;
import static org.cardanofoundation.hydra.core.utils.MoreJson.readValue;
import static org.cardanofoundation.hydra.core.utils.MoreJson.serialise;

@Slf4j
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

    private URI cardanoTransactionUrl() {
        return URI.create(format("%s/cardano-transaction", baseUrl));
    }

    public Mono<HydraProtocolParameters> fetchProtocolParameters() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(protocolParametersUrl())
                .GET()
                .build();

        CompletableFuture<HydraProtocolParameters> responseC = httpClient.sendAsync(request, BodyHandlers.ofString())
                .thenApply(r -> {
                    if (r.statusCode() != 200) {
                        var errorMessage = String.format("Error fetching protocol parameters, status code: %d, response body: %s",
                                r.statusCode(), r.body());
                        throw new HydraException(errorMessage);
                    }

                    return r.body();
                })
                .thenApply(responseBody -> readValue(responseBody, HydraProtocolParameters.class));

        return Mono.fromFuture(responseC)
                .timeout(timeout);
    }

    public Mono<HeadCommitResponse> commitRequest(Map<String, UTXO> commitDataMap) {
        var serialisedJson = serialise(commitDataMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(commitUrl())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serialisedJson))
                .build();

        return Mono.fromFuture(() -> {
                    return httpClient.sendAsync(request, BodyHandlers.ofString())
                            .thenApply(r -> {
                                if (r.statusCode() != 200) {
                                    var errorMessage = String.format("Error committing UTxOs, status code: %d, response body: %s",
                                            r.statusCode(), r.body());
                                    throw new HydraException(errorMessage);
                                }

                                return r.body();
                            })
                            .thenApply(responseBody -> readValue(responseBody, HeadCommitResponse.class));
                }
                ).timeout(timeout);
    }

}
