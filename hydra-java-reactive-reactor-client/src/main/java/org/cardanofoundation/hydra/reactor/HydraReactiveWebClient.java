package org.cardanofoundation.hydra.reactor;

import org.cardanofoundation.hydra.core.HydraException;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.cardanofoundation.hydra.core.model.http.HeadCommitResponse;
import org.cardanofoundation.hydra.core.model.http.HydraProtocolParameters;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static java.lang.String.format;

public class HydraReactiveWebClient {

    private final WebClient webClient;

    public HydraReactiveWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    private String commitUrl() {
        return format("/commit");
    }

    private String protocolParametersUrl() {
        return format("/protocol-parameters");
    }

    public Mono<HydraProtocolParameters> fetchProtocolParameters(Duration timeout) {
        return webClient.get()
                .uri(protocolParametersUrl())
                .retrieve()
                .bodyToMono(HydraProtocolParameters.class)
                .timeout(timeout)
                .onErrorMap(HydraReactiveWebClient::handleError);
    }

    public Mono<HeadCommitResponse> commitRequest(Map<String, UTXO> commitDataMap, Duration timeout) {
        return webClient.post()
                .uri(commitUrl())
                .body(BodyInserters.fromValue(commitDataMap))
                .retrieve()
                .bodyToMono(HeadCommitResponse.class)
                .timeout(timeout)
                .onErrorMap(HydraReactiveWebClient::handleError);
    }

    private static Throwable handleError(Throwable t) {
        return new HydraException("An error occurred during the WebClient request", t);
    }

}
