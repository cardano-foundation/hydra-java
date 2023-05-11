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
    <dependency>
        <groupId>org.cardanofoundation</groupId>
        <artifactId>hydra-java-high-level-client</artifactId>
        <version>0.0.5-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## Compatibility
This client is compatible with Hydra's master (unreleased version yet).

## Example usage

```
var wsUrl = "ws://localhost:4001"; // locally running hydra instance
var hydraClientOptions = HydraClientOptions.createDefault(wsUrl);
var hydraWSClient = new HydraWSClient(hydraClientOptions);
hydraWSClient.addHydraQueryEventListener(response -> log.info("response:{}", response));
hydraWSClient.addHydraStateEventListener((prev, now) -> log.info("prev:{}, now:{}", prev, now));
hydraWSClient.connectBlocking(60, TimeUnit.SECONDS);

System.out.println(hydraWSClient.getState()); // HydraState.Idle

hydraWSClient.init(); // fires init request for this client
// at least one client needs to initialise the network

System.out.println(hydraWSClient.getState()); // HydraState.HeadIsInitializing

// when all head participants commit their UTxOs then Hydra head is open, you can also commit empty UTxO but at least one head operator needs to commit // something
hydraWSClient.commit(); // commits empty UTxO

// time passes and then you will be able to see that HydraState becomes open

System.out.println(hydraWSClient.getState()); // HydraState.Open
```

## TODO
- Publish snapshot on maven
- JavaDocs and improve documentation
- Unit tests
- add integration tests for various scenarios
