package org.cardanofoundation.hydra.client.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UTXO {

    String address;
    Value value;

}

//             "address": "addr_test1vqg9ywrpx6e50uam03nlu0ewunh3yrscxmjayurmkp52lfskgkq5k",
//             "datum": null,
//             "datumhash": null,
//             "inlineDatum": null,
//             "referenceScript": null,
//             "value": {
//                 "lovelace": 500000000
//             }
