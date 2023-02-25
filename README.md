# hydra-java-client


This is an **incubator project**, which simplifies working with Hydra from java applications. Hydra is an isomorphic state machine L2 network, which works seemlessly with Cardano.


## Requirements
- JDK11
- maven3


## Building
```
git clone https://github.com/cardano-foundation/hydra-java-client
cd hydra-java-client
mvn clean install
```

## Hydra
You can access documentation regarding Hydra here: https://hydra.family/head-protocol/. We recommend especially to follow QuickStart on devnet network (https://hydra.family/head-protocol/docs/getting-started/quickstart) and (https://hydra.family/head-protocol/docs/getting-started/demo/with-docker) as well as reading (https://hydra.family/head-protocol/docs/tutorial/using_hydra/using-hydra-part-3)

Hydra API specs: https://hydra.family/head-protocol/api-reference

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

System.out.println(hydraWSClient.getState()); // HydraState.Open
```
