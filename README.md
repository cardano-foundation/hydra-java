# hydra-java

[![Build](https://github.com/cardano-foundation/hydra-java/actions/workflows/maven-build.yml/badge.svg)](https://github.com/cardano-foundation/hydra-java/actions/workflows/maven-build.yml)
[![License](https://img.shields.io:/github/license/cardano-foundation/hydra-java?label=license)](https://github.com/cardano-foundation/hydra-java/blob/master/LICENSE)
[![Discord](https://img.shields.io/discord/1022471509173882950)](https://discord.gg/4WVNHgQ7bP)

This is an **incubator project**, which simplifies working with Hydra from java applications. Hydra is an isomorphic state machine L2 network, which works seamlessly with Cardano.

## Hydra
You can access documentation regarding Hydra here: https://hydra.family/head-protocol/. We recommend especially to follow QuickStart on devnet network (https://hydra.family/head-protocol/docs/getting-started/quickstart) and (https://hydra.family/head-protocol/docs/getting-started/demo/with-docker) as well as reading (https://hydra.family/head-protocol/docs/tutorial/using_hydra/using-hydra-part-3)

Hydra API specs: https://hydra.family/head-protocol/api-reference

Hydra State Machine Diagram: https://hydra.family/head-protocol/core-concepts/behavior

## Requirements
- JDK17
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

## Version Compatibility Matrix

| Hydra Version | Library Version | Cardano Client Library Version | JDK VERSION |
|---------------|-----------------|--------------------------------|-------------|
| 0.10.x        | 0.0.6           | 0.4.x                          | JDK 11      |
| 0.10.x        | 0.0.7           | 0.5.x                          | JDK 11      |
| 0.13.x        | 0.0.9           | 0.5.x                          | JDK 17      |

## Dependency
```xml
<dependencies>
    <dependency>
        <groupId>org.cardanofoundation</groupId>
        <artifactId>hydra-java-client</artifactId>
        <version>0.0.9</version>
    </dependency>
    <dependency>
        <groupId>org.cardanofoundation</groupId>
        <artifactId>cardano-client-lib-adapter</artifactId>
        <version>0.0.9</version>
    </dependency>
    <dependency>
        <groupId>org.cardanofoundation</groupId>
        <artifactId>reactive-reactor-client</artifactId>
        <version>0.0.9</version>
    </dependency>
</dependencies>
```

## Project Missing features / functionality
The current version may not contain exactly what you need. If there is a missing feature / functionality, we happily accept pull requests. Ideally please discuss with us the idea first, file an issue and let's agree on design of it. Should pull request not be possible we are open to do the work for you provided the github issue is raised and documented well enough to understand it. 

## Additional Docs
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- [SECURITY.md](SECURITY.md)
- [CONTRIBUTING.md](CONTRIBUTING.md)
- [CHANGELOG.md](CHANGELOG.md)

## Modules

| Submodule                  | Goal / Description                                                         |
|----------------------------|----------------------------------------------------------------------------|
| hydra-java-core            | main classes  / code                                                       |
| hydra-java-client          | low level web socket client                                                |
| reactive-reactor-client    | experimental request / response high level reactive client (using reactor) |
| cardano-client-lib-adapter | bindings to BloxBean's cardano-client lib                                  |
| test-containers-support    | utilities to assist in testing via test containers project                 |

## Example usage (client)

```
var wsUrl = "ws://localhost:4001"; // locally running hydra instance
var hydraClientOptions = HydraClientOptions.builder(wsUrl)
                    .uTxOStore(new InMemoryUTxOStore())
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
