package it.davidlab.algonot.domain;

import java.util.Objects;

public class BlockchainData {

    private final String appVersion;
    private final String packetCode;
    private final String documentHash;
    private final String note;

    public BlockchainData(String appVersion, String packetCode, String documentHash, String note) {
        this.appVersion = appVersion;
        this.packetCode = packetCode;
        this.documentHash = documentHash;
        this.note = note;
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

    public String getNote() {
        return note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockchainData)) return false;
        BlockchainData that = (BlockchainData) o;
        return appVersion.equals(that.appVersion) && packetCode.equals(that.packetCode)
                && documentHash.equals(that.documentHash) && note.equals(that.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appVersion, packetCode, documentHash, note);
    }
}
