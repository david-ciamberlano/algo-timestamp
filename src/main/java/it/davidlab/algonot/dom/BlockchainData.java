package it.davidlab.algonot.dom;

public class BlockchainData {

    private final String appName;
    private final String appVersion;
    private final String packetName;
    private final String documentHash;

    public BlockchainData(String appName, String appVersion, String packetName, String documentHash) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.packetName = packetName;
        this.documentHash = documentHash;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public String getPacketName() {
        return packetName;
    }

    @Override
    public String toString() {
        return "BlockchainData{" +
                "appName='" + appName + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", packetName='" + packetName + '\'' +
                ", documentHash='" + documentHash + '\'' +
                '}';
    }
}
