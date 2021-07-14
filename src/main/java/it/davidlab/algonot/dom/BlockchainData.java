package it.davidlab.algonot.dom;

public class BlockchainData {

    private final String packetName;
    private final String documentHash;

    public BlockchainData(String documentHash, String packetName) {
        this.documentHash = documentHash;
        this.packetName = packetName;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public String getPacketName() {
        return packetName;
    }
}
