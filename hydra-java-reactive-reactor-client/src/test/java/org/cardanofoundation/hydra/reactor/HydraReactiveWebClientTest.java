package org.cardanofoundation.hydra.reactor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.cardanofoundation.hydra.core.model.UTXO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class HydraReactiveWebClientTest {

    private final static String PROTOCOL_PARAMETERS_RESPONSE = "{\"minFeeA\":0,\"minFeeB\":0,\"maxBlockBodySize\":65536,\"maxTxSize\":16384,\"maxBlockHeaderSize\":1100,\"keyDeposit\":400000,\"poolDeposit\":500000000,\"eMax\":18,\"nOpt\":50,\"a0\":0.1,\"rho\":1.78650067e-3,\"tau\":0.1,\"protocolVersion\":{\"major\":7,\"minor\":0},\"minPoolCost\":0,\"costmdls\":{},\"prices\":{\"prMem\":0,\"prSteps\":0},\"maxTxExUnits\":{\"exUnitsMem\":9223372036854775806,\"exUnitsSteps\":9223372036854775806},\"maxBlockExUnits\":{\"exUnitsMem\":80000000,\"exUnitsSteps\":40000000000},\"maxValSize\":5000,\"collateralPercentage\":150,\"maxCollateralInputs\":3,\"coinsPerUTxOByte\":0}⏎\n";

    private final static String HEAD_COMMIT_RESPONSE = "{\"cborHex\":\"84a7008382582088167818c3377fd1405880028693078a4cbf689d86834c18a71a4388cc8a216f008258209d3b99f4226b343ac3c33f72356a702c03bd792f6e36c59944bd693cb2f9c074018258209d3b99f4226b343ac3c33f72356a702c03bd792f6e36c59944bd693cb2f9c074040d818258209d3b99f4226b343ac3c33f72356a702c03bd792f6e36c59944bd693cb2f9c07404128182582033de8a815e3c2762402571d56124e2db905bf696934341ac2b0e580689ec64d6000182a300581d708dcc1fb34d1ba168dfb0b82e7d1a31956a2db5856f268146b0fd7f2a01821a03197500a1581c9031ff34954478aa3cdc34c9c86e2efec800a0a0aff968362250838fa1581cf8a68cd18e59a6ace848155a0e967af64f4d00cf8acee8adc95a6b0d010282005820efcb830f02acd98a6e3f5445dc5d08e6df905e2686061b85cf8b9b6ea68589c1a200581d60f8a68cd18e59a6ace848155a0e967af64f4d00cf8acee8adc95a6b0d011a00e400c0021a0035d8600e81581cf8a68cd18e59a6ace848155a0e967af64f4d00cf8acee8adc95a6b0d0b5820b6bbf9b19a4ff82c3504d3148b504116d51045d459a1bfd08616e38a9331402fa30081825820eb94e8236e2099357fa499bfbc415968691573f25ec77435b7949f5fdfaa5da05840902b11eb0bf16a4f9dd6cef019cbb87c6338845e471a8a35b983221f8035dd4ff16a141ac2ed145afe5c55d470cbcdfaffa68585639d416e6b3ed78a730d86090482581c9031ff34954478aa3cdc34c9c86e2efec800a0a0aff968362250838fd8799f5820b37aabd81024c043f53a069c91e51a5b52e4ea399ae17ee1fe3cb9c44db707eb9fd8799fd8799fd8799f582088167818c3377fd1405880028693078a4cbf689d86834c18a71a4388cc8a216fff00ff583cd8799fd8799fd8799f581c5e4e214a6addd337126b3a61faad5dfe1e4f14f637a8969e3a05eefdffd87a80ffa140a1401a02faf080d87980d87a80ffffff581c9031ff34954478aa3cdc34c9c86e2efec800a0a0aff968362250838fff0581840001d87a9f9fd8799fd8799f582088167818c3377fd1405880028693078a4cbf689d86834c18a71a4388cc8a216fff00ffffff821a00d59f801b00000002540be400f5f6\",\"description\":\"Hydra commit transaction\",\"type\":\"Tx BabbageEra\"}\n";

    private static WireMockServer wireMockServer;

    private HydraReactiveWebClient hydraReactiveWebClient;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    public void setUp() {
        WebClient webClient = WebClient.create("http://localhost:" + wireMockServer.port());
        hydraReactiveWebClient = new HydraReactiveWebClient(webClient);
    }

    @Test
    public void testFetchProtocolParameters() {
        // Mocking WireMock to respond with a static JSON
        stubFor(get(urlPathMatching("/protocol-parameters"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(PROTOCOL_PARAMETERS_RESPONSE)));

        var resultMono = hydraReactiveWebClient.fetchProtocolParameters(Duration.ofSeconds(1));

        StepVerifier.create(resultMono)
                .expectNextMatches(parameters -> {
                    // Add assertions based on the expected values in the static JSON file
                    return true;
                })
                .verifyComplete();
    }

    @Test
    public void testCommitRequest() {
        // Mocking WireMock to respond with a static JSON
        stubFor(post(urlPathMatching("/commit"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(HEAD_COMMIT_RESPONSE)));

        var commitDataMap = Collections.singletonMap("yourKey",
                UTXO.builder().address("addr_test1vp0yug22dtwaxdcjdvaxr74dthlpunc57cm639578gz7algset3fh").build());

        var resultMono = hydraReactiveWebClient.commitRequest(commitDataMap, Duration.ofSeconds(1));

        StepVerifier.create(resultMono)
                .expectNextMatches(response -> {
                    // Add assertions based on the expected values in the static JSON file
                    return true;
                })
                .verifyComplete();
    }

}
