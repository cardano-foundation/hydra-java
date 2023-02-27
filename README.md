# hydra-java-client

[![Build](https://github.com/cardano-foundation/hydra-java-client/actions/workflows/maven-build.yml/badge.svg)](https://github.com/cardano-foundation/hydra-java-client/actions/workflows/maven-build.yml)
[![CodeQL](https://github.com/cardano-foundation/hydra-java-client/actions/workflows/codeql.yml/badge.svg)](https://github.com/cardano-foundation/hydra-java-client/actions/workflows/codeql.yml)
![Maven Central](https://img.shields.io/maven-central/v/org.cardanofoundation/hydra-java-client)
[![License](https://img.shields.io:/github/license/cardano-foundation/hydra-java-client?label=license)](https://github.com/cardano-foundation/hydra-java-client/blob/master/LICENSE)
![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/cardano-foundation/hydra-java-client)
[![javadoc](https://javadoc.io/badge2/org.cardanofoundation/hydra-java-client/javadoc.svg)](https://javadoc.io/doc/org.cardanofoundation/hydra-java-client)
\
[![Discord](https://dcbadge.vercel.app/api/server/Pgrndv3A)](https://discord.gg/Pgrndv3A)


This is an **incubator project**, which simplifies working with Hydra from java applications. Hydra is an isomorphic state machine L2 network, which works seemlessly with Cardano.


## Hydra
You can access documentation regarding Hydra here: https://hydra.family/head-protocol/. We recommend especially to follow QuickStart on devnet network (https://hydra.family/head-protocol/docs/getting-started/quickstart) and (https://hydra.family/head-protocol/docs/getting-started/demo/with-docker) as well as reading (https://hydra.family/head-protocol/docs/tutorial/using_hydra/using-hydra-part-3)

Hydra API specs: https://hydra.family/head-protocol/api-reference

Hydra State Machine Diagram: https://hydra.family/head-protocol/core-concepts/behavior


## Requirements
- JDK11
- maven3

## Building
```
git clone https://github.com/cardano-foundation/hydra-java-client
cd hydra-java-client
mvn clean install
```

## Dependency
```xml
<dependency>
    <groupId>org.cardanofoundation</groupId>
    <artifactId>hydra-java-client</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Compatibility
This client is compatible with Hydra 0.8.1

## Example usage

```
var hydraWSClient = new HydraWSClient(new URI("ws://dev.cf-hydra-voting-poc.metadata.dev.cf-deployments.org:4001"));
hydraWSClient.setHydraQueryEventListener(response -> log.info("response:{}", response));
hydraWSClient.setHydraStateEventListener((prev, now) -> log.info("prev:{}, now:{}", prev, now));
hydraWSClient.connectBlocking(60, TimeUnit.SECONDS);

System.out.println(hydraWSClient.getState()); // HydraState.Idle

hydraWSClient.init(); // fires init request for this client
// at least one client needs to initialise the network

System.out.println(hydraWSClient.getState()); // HydraState.ReadyForCommit

// when all head participants commit their UTxOs then Hydra head is open, you can also commit empty UTxO but at least one head operator needs to commit // something
hydraWSClient.commit(); // commits empty UTxO

// time passes and then you will be able to see that HydraState becomes open

System.out.println(hydraWSClient.getState()); // HydraState.Open
```
