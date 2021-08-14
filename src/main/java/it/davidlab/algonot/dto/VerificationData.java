package it.davidlab.algonot.dto;

public class VerificationData {

    private final String appName;
    private final String appVersion;
    private final String packetCode;
    private final String documentHash;
    private final String senderAddr;
    private final long blockNum;

    public VerificationData(String appName, String appVersion, String packetCode, String documentHash,
                            String senderAddr, long blockNum) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.packetCode = packetCode;
        this.documentHash = documentHash;
        this.senderAddr = senderAddr;
        this.blockNum = blockNum;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getPacketCode() {
        return packetCode;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public String getSenderAddr() {
        return senderAddr;
    }

    public long getBlockNum() {
        return blockNum;
    }
}
