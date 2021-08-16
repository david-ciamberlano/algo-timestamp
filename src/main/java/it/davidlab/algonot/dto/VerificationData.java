package it.davidlab.algonot.dto;

public class VerificationData {

     private final String note;
    private final String appVersion;
     private final String packetCode;
     private final String documentHash;
     private final String senderAddr;
     private final long blockNum;
    private final long timestamp;

    public VerificationData(String note, String appVersion, String packetCode, String documentHash,
                            String senderAddr, long blockNum, long timestamp) {
        this.note = note;
        this.appVersion = appVersion;
        this.packetCode = packetCode;
        this.documentHash = documentHash;
        this.senderAddr = senderAddr;
        this.blockNum = blockNum;
        this.timestamp = timestamp;
    }

    public String getNote() {
        return note;
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

    public long getTimestamp() {
        return timestamp;
    }
}
