package it.davidlab.algonot.domain;

import java.util.Objects;

public class NotarizationCert {

    private final BlockchainData blockchainData;
    private final String originalFileName;
    private final long docSize;
    private final String creatorAddr;
    private final String txId;
    private final long blockNum;


    public NotarizationCert(BlockchainData blockchainData, String originalFileName, long docSize,
                            String creatorAddr, String txId, long blockNum) {
        this.blockchainData = blockchainData;
        this.originalFileName = originalFileName;
        this.docSize = docSize;
        this.creatorAddr = creatorAddr;
        this.txId = txId;
        this.blockNum = blockNum;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotarizationCert)) return false;
        NotarizationCert that = (NotarizationCert) o;
        return docSize == that.docSize && blockNum == that.blockNum && blockchainData.equals(that.blockchainData)
                && originalFileName.equals(that.originalFileName) && creatorAddr.equals(that.creatorAddr)
                && txId.equals(that.txId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockchainData, originalFileName, docSize, creatorAddr, txId, blockNum);
    }
}
