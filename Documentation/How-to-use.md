Info notarized in the blockchain (in the note field of a Tx):

- Packet Name = sha256Hex of: wallet address + document hash
- Document hash


To run the application:

just unzip the packet. ù
```
java -jar algotimestamp-1.0-beta.jar
```


## REST api

curl -X POST 'localhost:8080/api/notarize' --form 'file=@"application.properties"' --form 'note=' -O -J


