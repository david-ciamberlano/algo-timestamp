Info notarized in the blockchain (in the note field of a Tx):

- Packet Name = sha256Hex of: wallet address + document hash
- Document hash



## REST api

curl -X POST 'localhost:8080/api/notarize' --form 'file=@"application.properties"' --form 'note=' -O -J


