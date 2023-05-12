# hydra-java

[![Build](https://github.com/cardano-foundation/hydra-java/actions/workflows/maven-build.yml/badge.svg)](https://github.com/cardano-foundation/hydra-java/actions/workflows/maven-build.yml)
[![License](https://img.shields.io:/github/license/cardano-foundation/hydra-java?label=license)](https://github.com/cardano-foundation/hydra-java/blob/master/LICENSE)

This is an **incubator project**, which simplifies working with Hydra from java applications. Hydra is an isomorphic state machine L2 network, which works seamlessly with Cardano.

## Hydra
You can access documentation regarding Hydra here: https://hydra.family/head-protocol/. We recommend especially to follow QuickStart on devnet network (https://hydra.family/head-protocol/docs/getting-started/quickstart) and (https://hydra.family/head-protocol/docs/getting-started/demo/with-docker) as well as reading (https://hydra.family/head-protocol/docs/tutorial/using_hydra/using-hydra-part-3)

Hydra API specs: https://hydra.family/head-protocol/api-reference

Hydra State Machine Diagram: https://hydra.family/head-protocol/core-concepts/behavior

## Requirements
- JDK11
- maven3

## Building
```
git clone https://github.com/cardano-foundation/hydra-java
cd hydra-java
mvn clean install
```

## Running integration tests
```shell
mvn clean verify -P with-integration-tests
```

## Dependency
```xml
<dependencies>
    <dependency>
        <groupId>org.cardanofoundation</groupId>
        <artifactId>hydra-java-client</artifactId>
        <version>0.0.5-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Compatibility
This client is compatible with Hydra's master (unreleased version yet).

## Example usage

```
var wsUrl = "ws://localhost:4001"; // locally running hydra instance
var hydraClientOptions = HydraClientOptions.builder(wsUrl)
                    .withUTxOStore(new InMemoryUTxOStore())
                    .build()

var hydraWSClient = new HydraWSClient(hydraClientOptions);
var hydraLogger = SLF4JHydraLogger.of(log, "my_hydra_node");
hydraWSClient.addHydraQueryEventListener(hydraLogger);
hydraWSClient.addHydraStateEventListener(hydraLogger);
hydraWSClient.connectBlocking(60, TimeUnit.SECONDS);

System.out.println(hydraWSClient.getState()); // HydraState.Idle

hydraWSClient.init(); // fires init request for this client
// at least one client needs to initialise the network

System.out.println(hydraWSClient.getState()); // HydraState.HeadIsInitializing

// when all head participants commit their UTxOs then Hydra head is open, you can also commit empty UTxO but at least one head operator needs to commit // something

// commitment from L1 will mean that funds will be frozen on L1 by Hydra smart contract
var utxo = new UTXO();
utxo.setAddress("addr_test1vru2drx33ev6dt8gfq245r5k0tmy7ngqe79va69de9dxkrg09c7d3");
utxo.setValue(Map.of("lovelace", BigInteger.valueOf(1000 * 1_000_000))); // 1000 ADA

hydraWSClient.commit("ddf1db5cc1d110528828e22984d237b275af510dc82d0e7a8fc941469277e31e#0", utxo);

// time passes and then you will be able to see that HydraState becomes open

System.out.println(hydraWSClient.getState()); // HydraState.Open

// now one can send transactions and wait for confirmation...

// after finishing operator can close the head... after contenstation period remaining funds will be unlocked on L1
```

## TODO
- High-Level client implementation
- Publish snapshot / release via maven to Sonatype
- JavaDocs and improve documentation
- Unit tests
