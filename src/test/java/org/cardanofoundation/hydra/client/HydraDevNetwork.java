package org.cardanofoundation.hydra.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.stringtemplate.v4.ST;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.containers.BindMode.READ_WRITE;

@Slf4j
public class HydraDevNetwork implements Startable {

    private final static String ISO_8601BASIC_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String INPUT_OUTPUT_CARDANO_NODE = "inputoutput/cardano-node:1.35.7";

    private static final String INPUT_OUTPUT_HYDRA_NODE = "ghcr.io/input-output-hk/hydra-node:unstable";

    protected final static ObjectMapper objectMapper = new ObjectMapper();

    private final static String MARKER_DATUM_HASH = "a654fb60d21c1fed48db2c320aa6df9737ec0204c0ba53b9b94a09fb40e757f3";

    public final static int CARDANO_REMOTE_PORT = 3001;

    public final static int HYDRA_ALICE_REMOTE_PORT = 4001;

    public final static int HYDRA_BOB_REMOTE_PORT = 4002;

    protected GenericContainer<?> cardanoContainer;

    protected GenericContainer<?> aliceHydraContainer;

    protected GenericContainer<?> bobHydraContainer;

    public HydraDevNetwork() {
        this.cardanoContainer = createCardanoNodeContainer();
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

        log.info("ReferenceScriptsTxId:{}", referenceScriptsTxId);

        var hydraNet = Network.builder().createNetworkCmdModifier(createNetworkCmd -> {
            createNetworkCmd.withName("hydra_net");
            createNetworkCmd.withAttachable(true);
        }).build();

        this.aliceHydraContainer = createAliceHydraNode(cardanoContainer, referenceScriptsTxId, hydraNet);
        this.bobHydraContainer = createBobHydraNode(cardanoContainer, referenceScriptsTxId, hydraNet);

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

    public static String getHydraApiUrl(GenericContainer<?> container, int port) {
        var host = container.getHost();
        var mappedPort = container.getMappedPort(port);

        return String.format("ws://%s:%d", host, mappedPort);
    }

    // docker run --rm -it -v ./devnet:/devnet ghcr.io/input-output-hk/hydra-node:unstable publish-scripts --testnet-magic 42 --node-socket /devnet/node.socket --cardano-signing-key /devnet/credentials/faucet.sk
    private String publishReferenceScripts(GenericContainer<?> cardanoContainer) {
        StringBuilder commandOutputBuilder = new StringBuilder();
        var hydraCliContainer = new GenericContainer<>(INPUT_OUTPUT_HYDRA_NODE)
                .withVolumesFrom(cardanoContainer, READ_WRITE)
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
                        "--node-socket", "/devnet/node.socket",
                        "--cardano-signing-key", "/devnet/credentials/faucet.sk"
                );

        hydraCliContainer.start();

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
        return new GenericContainer<>(INPUT_OUTPUT_CARDANO_NODE)
                .withClasspathResourceMapping("/devnet",
                        "/devnet",
                        READ_WRITE
                )
                //.withFileSystemBind("/tmp", "/tmp", READ_WRITE)
                .withEnv(Map.of(
                        "CARDANO_BLOCK_PRODUCER", "true",
                        "CARDANO_NODE_SOCKET_PATH", "/devnet/node.socket",
                        "CARDANO_SOCKET_PATH", "/devnet/node.socket"
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
                );
    }

    protected GenericContainer<?> createAliceHydraNode(GenericContainer<?> cardanoContainer, String scriptsTxId, Network hydraNet) {
        String containerName = "alice-hydra-node";

        return new GenericContainer<>(INPUT_OUTPUT_HYDRA_NODE)
                .withExposedPorts(4001)
                .withAccessToHost(true)

                .withClasspathResourceMapping("/keys",
                        "/keys",
                        READ_ONLY
                )
                .withVolumesFrom(cardanoContainer, READ_WRITE)
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix(containerName))
                .waitingFor(Wait.forHttp("/").forStatusCode(400)) // TODO clever websocket check, e.g. peer connected
                .withEnv(Map.of("HYDRA_SCRIPTS_TX_ID", scriptsTxId))
                .withNetwork(hydraNet)
                .withNetworkAliases(containerName)

                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName).withHostName(containerName).withAliases(containerName))
                .withCommand(
                        "-q",
                        "--node-id", "1"
                        , "--api-host", "0.0.0.0"
                        , "--monitoring-port", "6001"
                        , "--port", "5001"
                        , "--api-port", "4001"
                        , "--peer", "localhost:5002"
                        //, "--host", "172.16.238.10"
                        , "--hydra-scripts-tx-id", scriptsTxId
                        , "--hydra-signing-key", "/keys/alice.sk"
                        , "--hydra-verification-key", "/keys/bob.vk"
                        , "--cardano-signing-key", "/devnet/credentials/alice.sk"
                        , "--cardano-verification-key", "/devnet/credentials/bob.vk"
                        , "--ledger-genesis", "/devnet/genesis-shelley.json"
                        , "--ledger-protocol-parameters", "/devnet/protocol-parameters.json"
                        , "--persistence-dir", "/tmp/alice-hydra-node_db" + System.currentTimeMillis()
                        , "--testnet-magic", "42"
                        , "--node-socket", "/devnet/node.socket");
    }

    protected GenericContainer<?> createBobHydraNode(GenericContainer<?> cardanoContainer, String scriptsTxId, Network hydraNet) {
        String containerName = "bob-hydra-node";

        return new GenericContainer<>(INPUT_OUTPUT_HYDRA_NODE)
                .withExposedPorts(4002)
                .withAccessToHost(true)
                .withVolumesFrom(cardanoContainer, READ_WRITE)
                .withClasspathResourceMapping("/keys",
                        "/keys",
                        READ_ONLY
                )
                .withLogConsumer(new Slf4jLogConsumer(log).withPrefix(containerName))
                .waitingFor(new DockerHealthcheckWaitStrategy())
                .waitingFor(Wait.forHttp("/").forStatusCode(400)) // TODO make clever websocket check
                .withEnv(Map.of("HYDRA_SCRIPTS_TX_ID", scriptsTxId))
                .withNetwork(hydraNet)
                .withNetworkAliases(containerName)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName).withHostName(containerName).withAliases(containerName))
                .withCommand(
                        "-q",
                        "--node-id", "2"
                        , "--api-host", "0.0.0.0"
                        , "--monitoring-port", "6002"
                        , "--api-port", "4002"
                        , "--port", "5002"
                        , "--peer", "localhost:5001"
                        //, "--host", "172.16.238.20"
                        , "--hydra-scripts-tx-id", scriptsTxId
                        , "--hydra-signing-key", "/keys/bob.sk"
                        , "--hydra-verification-key", "/keys/alice.vk"
                        , "--cardano-signing-key", "/devnet/credentials/bob.sk"
                        , "--cardano-verification-key", "/devnet/credentials/alice.vk"
                        , "--ledger-genesis", "/devnet/genesis-shelley.json"
                        , "--ledger-protocol-parameters", "/devnet/protocol-parameters.json"
                        , "--persistence-dir", "/tmp/bob-hydra-node_db" + System.currentTimeMillis()
                        , "--testnet-magic", "42"
                        , "--node-socket", "/devnet/node.socket");
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

        seedActor(cardanoContainer, faucetAddr, "alice", 1000, false);
        seedActor(cardanoContainer, faucetAddr, "alice", 100, true);

        seedActor(cardanoContainer, faucetAddr, "bob", 500, false);
        seedActor(cardanoContainer, faucetAddr, "bob", 100, true);
    }

}
