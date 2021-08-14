package it.davidlab.algonot.domain;

import java.util.Objects;

public class BlockchainData {

    private final String appName;
    private final String appVersion;
    private final String packetCode;
    private final String documentHash;

    public BlockchainData(String appName, String appVersion, String packetCode, String documentHash) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.packetCode = packetCode;
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

    public String getPacketCode() {
        return packetCode;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockchainData)) return false;
        BlockchainData that = (BlockchainData) o;
        return appName.equals(that.appName) && appVersion.equals(that.appVersion)
                && packetCode.equals(that.packetCode) && documentHash.equals(that.documentHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, appVersion, packetCode, documentHash);
    }
}
