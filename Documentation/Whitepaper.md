# Algo Timestamp

Algo-timestamp is a software to Notarize documents on the Algorand blockchain.
It produces a packet (just a zip file with a very long name) that can be stored anywhere and that contains:
- a copy of the notarized document
- a certificate (in json format) that contains all the information needed to verify the notarization.

## Why Notarization on the blockchain
The advantages of adopting blockchain technology for the Notarization are various:

- You don’t need an external certification authority since blockchain is a decentralized system
- You can obtain (and verify) proof of existence and proof of immutability in a single atomic transaction
- Blockchain is secure by design and reachable from all over the world
- Blockchain can store additional information on the document.
- You have to pay the transaction only once, for an unlimited time, and the cost is only a fraction of the equivalent cost of the digital signature and legal timestamp.


## How algo-timestamp works
This application focuses on two aspects:

- proof of existence
- proof of ownership

### Proof of existence
This serves to prove that a document existed at a certain time and has not been modified afterwards. We can do that in two steps:

1) compute the hash of the document 
2) send (store) this hash in a blockchain transaction

Each Algorand transaction can contain up to 1kb of data (bytes) in its note field. The Notarization extension uses this field to store a json object that contains the previously computed hash and other information on the document.
Once the hash is sealed in a transaction, we can guarantee that the document has not changed simply comparing its hash with the one in the blockchain. If only one bit is altered, the document hash will be completely different.
This is exactly the same procedure used by the digital signature process.

### Proof of ownership
One of the fields registered in the transaction is the sender’s account address. Since the sender is the only one who has the private key of that address, he is the only one who can certify that he has made that specific transaction at that specific time toward that specific wallet.

## The Algo-timestamp Packet
After each notarization
The notarization packet is just a zip file (with a very long name).
It contains a copy of the notarized document and a certificate file (in json format).

