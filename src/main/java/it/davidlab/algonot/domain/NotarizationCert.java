package it.davidlab.algonot.domain;

import org.apache.commons.io.FileUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class NotarizationCert {

    private final BlockchainData blockchainData;
    private final String originalFileName;
    private final long docSize;
    private final String creatorAddr;
    private final String txId;
    private final long blockNum;
    private final long timestamp;


    public NotarizationCert(BlockchainData blockchainData, String originalFileName, long docSize, String creatorAddr,
                            String txId, long blockNum, long timestamp) {
        this.blockchainData = blockchainData;
        this.originalFileName = originalFileName;
        this.docSize = docSize;
        this.creatorAddr = creatorAddr;
        this.txId = txId;
        this.blockNum = blockNum;
        this.timestamp = timestamp;
    }

    public BlockchainData getBlockchainData() {
        return blockchainData;
    }

    public String getCreatorAddr() {
        return creatorAddr;
    }

    public String getTxId() {
        return txId;
    }

    public long getBlockNum() {
        return blockNum;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public long getDocSize() {
        return docSize;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getHRSize() {
        return FileUtils.byteCountToDisplaySize(docSize);
    }

    public String getFormattedDate() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss O");
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()).format(dateFormatter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotarizationCert)) return false;
        NotarizationCert that = (NotarizationCert) o;
        return docSize == that.docSize && blockNum == that.blockNum && timestamp == that.timestamp && blockchainData.equals(that.blockchainData) && originalFileName.equals(that.originalFileName) && creatorAddr.equals(that.creatorAddr) && txId.equals(that.txId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockchainData, originalFileName, docSize, creatorAddr, txId, blockNum, timestamp);
    }
}
