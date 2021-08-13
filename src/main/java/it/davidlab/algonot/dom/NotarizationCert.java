package it.davidlab.algonot.dom;

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


    public boolean checkNull() {
        return
            blockchainData == null ||
            originalFileName  == null ||
            docSize  == 0 ||
            creatorAddr  == null ||
            txId  == null ||
            blockNum  == 0;
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
    public String toString() {
        return "NotarizationCert{" +
                "blockchainData=" + blockchainData +
                ", originalFileName='" + originalFileName + '\'' +
                ", docSize=" + docSize +
                ", creatorAddr='" + creatorAddr + '\'' +
                ", txId='" + txId + '\'' +
                ", blockNum=" + blockNum +
                '}';
    }
}
