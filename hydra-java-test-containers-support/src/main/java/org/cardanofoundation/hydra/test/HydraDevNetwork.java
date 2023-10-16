package org.cardanofoundation.hydra.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.stringtemplate.v4.ST;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.containers.BindMode.READ_WRITE;

@Slf4j
public class HydraDevNetwork implements Startable {

    private final static String ISO_8601BASIC_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String INPUT_OUTPUT_CARDANO_NODE = "inputoutput/cardano-node:8.1.2";

    private static final String INPUT_OUTPUT_HYDRA_NODE = "ghcr.io/input-output-hk/hydra-node:0.13.0";

    protected final static ObjectMapper objectMapper = new ObjectMapper();

    private final static String MARKER_DATUM_HASH = "a654fb60d21c1fed48db2c320aa6df9737ec0204c0ba53b9b94a09fb40e757f3";

    public final static int CARDANO_REMOTE_PORT = 3001;

    public final static int HYDRA_API_REMOTE_PORT = 4001;

    private final boolean cardanoLogging;

    private final boolean hydraLogging;

    private final Map<String, Integer> initialFunds;

    @Getter
    protected GenericContainer<?> cardanoContainer;

    @Getter
    protected GenericContainer<?> aliceHydraContainer;

    @Getter
    protected GenericContainer<?> bobHydraContainer;

    public HydraDevNetwork(boolean withCardanoLogging,
                           boolean withHydraLogging,
                           Map<String, Integer> initialFunds) {
        this.cardanoLogging = withCardanoLogging;
        this.hydraLogging = withHydraLogging;
        this.initialFunds = initialFunds;
        this.cardanoContainer = createCardanoNodeContainer();
    }

    public HydraDevNetwork(boolean withCardanoLogging,
                           boolean withHydraLogging) {
        this.cardanoLogging = withCardanoLogging;
        this.hydraLogging = withHydraLogging;
        this.initialFunds = getInitialFunds();
        this.cardanoContainer = createCardanoNodeContainer();
    }

    public HydraDevNetwork() {
        this(false, false, getInitialFunds());
    }

    public static Map<String, Integer> getInitialFunds() {
        var initialFunds = new LinkedHashMap<String, Integer>();
        initialFunds.put("alice", 100);
        initialFunds.put("bob", 100);
        initialFunds.put("alice-funds", 100);
        initialFunds.put("bob-funds", 100);

        return initialFunds;
    }

    public String getCardanoLocalSocketPath() {
        return "/devnet/node.socket";
    }

    public String getRemoteCardanoLocalSocketPath() {
        //return "/devnet/node.socket";
        return getClass().getClassLoader().getResource("devnet/node.socket").getFile();
    }

    @Override
    @SneakyThrows
    public void start() {
        log.info("Preparing devnet (cardano-node, hydra)...");
        prepareDevNet();

        log.info("Starting cardano node...");
        this.cardanoContainer.start();

        log.info("Seeding actors with initial funds...");
        seedActors(cardanoContainer);

        log.info("Publishing Hydra contract scripts to devnet cardano network.");
        var referenceScriptsTxId = publishReferenceScripts(cardanoContainer);

        log.info("ReferenceScriptsTxId: {}", referenceScriptsTxId);

        var network = Network.builder()
                .driver("bridge")
                .createNetworkCmdModifier(cmd -> {
                    var ipamConfig = new com.github.dockerjava.api.model.Network.Ipam.Config();
                    ipamConfig.withSubnet("172.16.238.0/24");
                    ipamConfig.withGateway("172.16.238.1");
                    ipamConfig.setNetworkID("hydra_net");

                    var ipam = new com.github.dockerjava.api.model.Network.Ipam();
                    ipam.withConfig(ipamConfig);

                    cmd.withName("hydra_net")
                    .withIpam(ipam);
                })
                .build();

        log.info("Creating network:" + network);

        this.aliceHydraContainer = createAliceHydraNode(cardanoContainer, referenceScriptsTxId, network);
        this.bobHydraContainer = createBobHydraNode(cardanoContainer, referenceScriptsTxId, network);

        this.aliceHydraContainer.dependsOn(cardanoContainer);
        this.bobHydraContainer.dependsOn(cardanoContainer);

        log.info("Starting alice and bob hydra nodes in parallel...");
        Startables.deepStart(aliceHydraContainer, bobHydraContainer).get();
    }

    @Override
    @SneakyThrows
    public void stop() {
        log.info("Cleaning up container resources...");

        if (aliceHydraContainer != null && aliceHydraContainer.isRunning()) {
            aliceHydraContainer.stop();
            aliceHydraContainer = null;
        }

        if (bobHydraContainer != null && bobHydraContainer.isRunning()) {
            bobHydraContainer.stop();
            bobHydraContainer = null;
        }

        if (cardanoContainer != null && cardanoContainer.isRunning()) {
            cardanoContainer.stop();
            cardanoContainer = null;
        }

        var devnetPath = Resources.getResource("devnet").getPath();

        java.nio.file.Files.deleteIfExists(Paths.get(devnetPath, "node.socket"));
        java.nio.file.Files.deleteIfExists(Paths.get(devnetPath, "genesis-byron.json"));
        java.nio.file.Files.deleteIfExists(Paths.get(devnetPath, "genesis-shelley.json"));
    }

    public static String getHydraApiWebSocketUrl(GenericContainer<?> container) {
        var host = container.getHost();
        var mappedPort = container.getMappedPort(HYDRA_API_REMOTE_PORT);

        return String.format("ws://%s:%d", host, mappedPort);
    }

    public static String getHydraApiWebUrl(GenericContainer<?> container) {
        var host = container.getHost();
        var mappedPort = container.getMappedPort(HYDRA_API_REMOTE_PORT);

        return String.format("http://%s:%d", host, mappedPort);
    }

    public int getCardanoPort() {
        return getCardanoContainer().getMappedPort(CARDANO_REMOTE_PORT);
    }

    // docker run --rm -it -v ./devnet:/devnet ghcr.io/input-output-hk/hydra-node:unstable publish-scripts --testnet-magic 42 --node-socket /devnet/node.socket --cardano-signing-key /devnet/credentials/faucet.sk
    private String publishReferenceScripts(GenericContainer<?> cardanoContainer) {
        StringBuilder commandOutputBuilder = new StringBuilder();
        try (var hydraCliContainer = new GenericContainer<>(INPUT_OUTPUT_HYDRA_NODE)) {
            hydraCliContainer.withVolumesFrom(cardanoContainer, READ_WRITE)
                    .withStartupCheckStrategy(
                            new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(60))
                    )
                    .withLogConsumer(new Slf4jLogConsumer(log).andThen(line -> {
                         if (line.getType() == OutputFrame.OutputType.STDOUT) {
                            commandOutputBuilder.append(line.getUtf8String());
                        }
                    }))
                    .withCommand(
                            "publish-scripts",
                            "--testnet-magic", "42",
                            "--node-socket", getCardanoLocalSocketPath(),
                            "--cardano-signing-key", "/devnet/credentials/faucet.sk"
                    );

            if (cardanoLogging) {
                hydraCliContainer.withLogConsumer(new Slf4jLogConsumer(log).withSeparateOutputStreams());
            }

            hydraCliContainer.start();
        }

        log.info("Publishing reference scripts...");

        return commandOutputBuilder.toString().replace("\n", "");
    }

    protected void prepareDevNet() throws IOException {
        var devnetPath = Resources.getResource("devnet").getPath();
        var byronFile = "genesis-byron.json";
        var shelleyFile = "genesis-shelley.json";
        var vrfKeyFile = "vrf.skey";

        ST byronST = new ST(Files.toString(new File(devnetPath, byronFile + ".tmpl"), UTF_8));
        ST shelleyST = new ST(Files.toString(new File(devnetPath, shelleyFile + ".tmpl"), UTF_8));

        var now = ZonedDateTime.now(UTC);
        byronST.add("START_TIME", now.toLocalDateTime().toInstant(UTC).getEpochSecond());
        shelleyST.add("START_TIME", now.format(DateTimeFormatter.ofPattern(ISO_8601BASIC_DATE_PATTERN)));

        java.nio.file.Files.deleteIfExists(Paths.get(devnetPath, byronFile));
        java.nio.file.Files.deleteIfExists(Paths.get(devnetPath, shelleyFile));

        var byron = java.nio.file.Files.write(Paths.get(devnetPath, byronFile), byronST.render().getBytes());
        var shelley = java.nio.file.Files.write(Paths.get(devnetPath, shelleyFile), shelleyST.render().getBytes());
        var vrfPath = Paths.get(devnetPath, vrfKeyFile);

        new File(byron.toUri()).setReadOnly();
        new File(shelley.toUri()).setReadOnly();

        // this is needed because vrf file is needs to be accessible obly by the owner!
        java.nio.file.Files.setPosixFilePermissions(vrfPath, PosixFilePermissions.fromString("rw-------"));
    }

    protected GenericContainer<?> createCardanoNodeContainer() {
        //val mem = 32 * 1024L * 1024L * 1024L;
        try (var cardanoNode = new GenericContainer<>(INPUT_OUTPUT_CARDANO_NODE)) {
                cardanoNode
                        .withExposedPorts(3001)
                        .withAccessToHost(true)
                        .withClasspathResourceMapping("/devnet",
                    "/devnet",
                    READ_WRITE
            )
                    .withEnv(Map.of(
                            "CARDANO_BLOCK_PRODUCER", "true",
                            "CARDANO_NODE_SOCKET_PATH", getCardanoLocalSocketPath(),
                            "CARDANO_SOCKET_PATH", getCardanoLocalSocketPath()
                    ))
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("cardano-node").withSeparateOutputStreams())
                    .withExposedPorts(CARDANO_REMOTE_PORT)
                    .waitingFor(Wait.forListeningPort())
                    .withCommand(
                            "run"
                            , "--config", "/devnet/cardano-node.json"
                            , "--topology", "/devnet/topology.json"
                            , "--database-path", "/tmp/cardano-db_" + System.currentTimeMillis()
                            , "--shelley-kes-key", "/devnet/kes.skey"
                            , "--shelley-vrf-key", "/devnet/vrf.skey"
                            , "--shelley-operational-certificate", "/devnet/opcert.cert"
                            , "--byron-delegation-certificate", "/devnet/byron-delegation.cert"
                            , "--byron-signing-key", "/devnet/byron-delegate.key"
                    )
                    .addExposedPort(CARDANO_REMOTE_PORT);

                return cardanoNode;
        }
    }

    protected GenericContainer<?> createAliceHydraNode(GenericContainer<?> cardanoContainer, String scriptsTxId, Network network) {
        String containerName = "hydra-node-alice";

        try (var aliceHydraNode = new GenericContainer<>(INPUT_OUTPUT_HYDRA_NODE)) {
                aliceHydraNode.withExposedPorts(4001, 5001)
                    .withAccessToHost(true)
                    .withNetwork(network)
                    .withNetworkAliases(containerName)
                    .withClasspathResourceMapping("/keys",
                            "/keys",
                            READ_ONLY
                    )
                    .withVolumesFrom(cardanoContainer, READ_WRITE)
                    .waitingFor(Wait.forLogMessage(".+Required subscriptions started.+", 1).withStartupTimeout(Duration.ofMinutes(1)))
                    .withEnv(Map.of("HYDRA_SCRIPTS_TX_ID", scriptsTxId))

                    .withCreateContainerCmdModifier(cmd -> {
                        cmd.withName(containerName)
                                .withHostName(containerName)
                                .withAliases(containerName)
                                .withIpv4Address("172.16.238.2");
                    })
                    .withCommand(
                            "--node-id", "alice"
                            , "--api-host", "0.0.0.0"
                            , "--host", "172.16.238.2"
                            , "--monitoring-port", "6001"
                            , "--peer", "hydra-node-bob:5001"
                            , "--hydra-scripts-tx-id", scriptsTxId
                            , "--hydra-signing-key", "/keys/alice.sk"
                            , "--hydra-verification-key", "/keys/bob.vk"
                            , "--cardano-signing-key", "/devnet/credentials/alice.sk"
                            , "--cardano-verification-key", "/devnet/credentials/bob.vk"
                            , "--ledger-protocol-parameters", "/devnet/protocol-parameters.json"
                            , "--persistence-dir", "/tmp/alice-hydra-node_db" + System.currentTimeMillis()
                            , "--testnet-magic", "42"
                            , "--node-socket", "/devnet/node.socket"
                            //, "--quiet"
                    );

            if (hydraLogging) {
                aliceHydraNode.withLogConsumer(new Slf4jLogConsumer(log).withSeparateOutputStreams());
            }

            return aliceHydraNode;
        }

    }

    protected GenericContainer<?> createBobHydraNode(GenericContainer<?> cardanoContainer, String scriptsTxId, Network network) {
        String containerName = "hydra-node-bob";

        try (var bobHydraNode = new GenericContainer<>(INPUT_OUTPUT_HYDRA_NODE)) {
                bobHydraNode.withExposedPorts(4001, 5001)
                    .withAccessToHost(true)
                    .withNetwork(network)
                    .withNetworkAliases(containerName)
                    .withVolumesFrom(cardanoContainer, READ_WRITE)
                    .withClasspathResourceMapping("/keys",
                            "/keys",
                            READ_ONLY
                    )
                    .waitingFor(Wait.forLogMessage(".+Required subscriptions started.+", 1).withStartupTimeout(Duration.ofMinutes(1)))
                    .withEnv(Map.of("HYDRA_SCRIPTS_TX_ID", scriptsTxId))
                    .withCreateContainerCmdModifier(cmd -> {
                        cmd.withName(containerName)
                        .withHostName(containerName)
                        .withAliases(containerName)
                        .withIpv4Address("172.16.238.3");
                    })
                    .withCommand(
                            "--node-id", "bob"
                            , "--api-host", "0.0.0.0"
                            , "--host", "172.16.238.3"
                            , "--monitoring-port", "6001"
                            , "--api-port", "4001"
                            , "--peer", "hydra-node-alice:5001"
                            , "--hydra-scripts-tx-id", scriptsTxId
                            , "--hydra-signing-key", "/keys/bob.sk"
                            , "--hydra-verification-key", "/keys/alice.vk"
                            , "--cardano-signing-key", "/devnet/credentials/bob.sk"
                            , "--cardano-verification-key", "/devnet/credentials/alice.vk"
                            , "--ledger-protocol-parameters", "/devnet/protocol-parameters.json"
                            , "--persistence-dir", "/tmp/bob-hydra-node_db" + System.currentTimeMillis()
                            , "--testnet-magic", "42"
                            , "--node-socket", "/devnet/node.socket"
                            //, "--quiet"
                    );

                if (hydraLogging) {
                    bobHydraNode.withLogConsumer(new Slf4jLogConsumer(log).withSeparateOutputStreams());
                }

                return bobHydraNode;
        }
    }

    protected void seedActor(GenericContainer<?> cardanoNodeContainer, String faucetAddress, String actor, int adaAmount, boolean marker) throws IOException, InterruptedException {
        log.info(String.format("Seeding a UTXO from faucet to %s with %d ADA, faucet address:%s ", actor, adaAmount, faucetAddress));
        val actorLovelaces = adaAmount * 1_000_000L;

        val faucetUTxOExecResult = cardanoNodeContainer.execInContainer("cardano-cli", "query", "utxo", "--testnet-magic", "42", "--address", faucetAddress, "--out-file", "/dev/stdout");
        if (faucetUTxOExecResult.getExitCode() != 0) {
            throw new RuntimeException("Unable to find faucet's UTxO, error:" + faucetUTxOExecResult);
        }
        val faucetUTxO = objectMapper.readTree(faucetUTxOExecResult.getStdout()).fieldNames().next();
        log.info("Faucet utxo:{}", faucetUTxO);

        log.info("Fetching address for actor:{}...", actor);
        val actorAddressExecResult = cardanoNodeContainer.execInContainer("cardano-cli", "address", "build", "--testnet-magic", "42", "--payment-verification-key-file", String.format("/devnet/credentials/%s.vk", actor));
        if (actorAddressExecResult.getExitCode() != 0) {
            throw new RuntimeException("Unable to find actor's address, error:" + actorAddressExecResult);
        }
        val actorAddr = actorAddressExecResult.getStdout();

        log.info("Actor's:{} address:{}", actor, actorAddr);

        log.info("Seeding actor:{}...", actor);

        var txBuildExecResult = marker ? cardanoNodeContainer.execInContainer("cardano-cli",
                "transaction", "build",
                "--testnet-magic", "42",
                "--babbage-era", "--cardano-mode",
                "--change-address", faucetAddress,
                "--tx-in", faucetUTxO,
                "--tx-out", String.format("%s+%d", actorAddr, actorLovelaces),
                "--tx-out-datum-hash", MARKER_DATUM_HASH,
                "--out-file", String.format("/tmp/seed-%s.draft", actor)
        )
                :
                cardanoNodeContainer.execInContainer("cardano-cli",
                        "transaction", "build",
                        "--testnet-magic", "42",
                        "--babbage-era", "--cardano-mode",
                        "--change-address", faucetAddress,
                        "--tx-in", faucetUTxO,
                        "--tx-out", String.format("%s+%d", actorAddr, actorLovelaces),
                        "--out-file", String.format("/tmp/seed-%s.draft", actor)
                );

        if (txBuildExecResult.getExitCode() != 0) {
            throw new RuntimeException("Unable to build transaction for actor, error:" + txBuildExecResult);
        }

        val txSignExecResult = cardanoNodeContainer.execInContainer("cardano-cli",
                "transaction", "sign",
                "--testnet-magic", "42",
                "--tx-body-file", String.format("/tmp/seed-%s.draft", actor),
                "--signing-key-file", "/devnet/credentials/faucet.sk",
                "--out-file", String.format("/tmp/seed-%s.signed", actor));

        if (txSignExecResult.getExitCode() != 0) {
            throw new RuntimeException("Unable to sign transaction for actor, error:" + txSignExecResult);
        }

        val txSeedIdExecResult = cardanoNodeContainer.execInContainer("cardano-cli",
                "transaction", "txid",
                "--tx-file", String.format("/tmp/seed-%s.signed", actor));

        if (txSeedIdExecResult.getExitCode() != 0) {
            throw new RuntimeException("Unable to get transaction id for actor, error:" + txSeedIdExecResult);
        }

        val txSeedIn = txSeedIdExecResult.getStdout().replace("\n", "") + "#0"; // first UTxO

        val txSubmitExecResult = cardanoNodeContainer.execInContainer("cardano-cli",
                "transaction", "submit",
                "--testnet-magic", "42",
                "--tx-file", String.format("/tmp/seed-%s.signed", actor));

        if (txSubmitExecResult.getExitCode() != 0) {
            throw new RuntimeException("Unable to submit transaction, error:" + txSubmitExecResult);
        }

        for (int i = 0; i < 10; i++) {
            log.info("Checking if actor:{} got ADA.", actor);
            Thread.sleep(500);

            val txQueryExecResult = cardanoNodeContainer.execInContainer("cardano-cli",
                    "query", "utxo",
                    "--testnet-magic", "42",
                    "--tx-in", txSeedIn,
                    "--out-file", "/dev/stdout");

            var foundTx = objectMapper.readTree(txQueryExecResult.getStdout()).has(txSeedIn);
            if (foundTx) {
                log.info("Actor:{} got ADA, sitting in the utxo:{}", actor, txSeedIn);
                break;
            }
            log.info("Actor:{} didn't get ADA, didn't find transaction's utxo: {} yet, will try again in 500ms...", actor, txSeedIn);
        }
    }

    private static String getFaucetDetails(GenericContainer<?> cardanoNodeContainer) throws IOException, InterruptedException {
        log.info("Fetching address for faucet...");

        val fauceutAddressExecResult = cardanoNodeContainer.execInContainer("cardano-cli", "address", "build", "--testnet-magic", "42", "--payment-verification-key-file", "/devnet/credentials/faucet.vk");
        if (fauceutAddressExecResult.getExitCode() != 0) {
            throw new RuntimeException("Unable to find faucet's address, error:" + fauceutAddressExecResult);
        }

        return fauceutAddressExecResult.getStdout();
    }

    protected void seedActors(GenericContainer<?> cardanoContainer) throws IOException, InterruptedException {
        String faucetAddr = getFaucetDetails(cardanoContainer);

        log.info("Faucet address:{}", faucetAddr);

        for (Map.Entry<String, Integer> entry : initialFunds.entrySet()) {
            String actor = entry.getKey();
            Integer ada = entry.getValue();
            seedActor(cardanoContainer, faucetAddr, actor, ada, false);
            seedActor(cardanoContainer, faucetAddr, actor, ada, false);
        }

        seedActor(cardanoContainer, faucetAddr, "alice", 100, true);
        seedActor(cardanoContainer, faucetAddr, "bob", 100, true);
    }

}
