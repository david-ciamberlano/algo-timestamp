package it.davidlab.algonot.dom;

import java.util.Date;

public class AlgoNotarizationCert {

    private final AlgoNotarizationReq notarizationData;
    private final String creatorAddr;
    private final String txId;

    public AlgoNotarizationCert(AlgoNotarizationReq notarizationReq, String addr, String txId) {
        this.notarizationData = notarizationReq;
        this.creatorAddr = addr;
        this.txId = txId;
    }

    public AlgoNotarizationReq getNotarizationData() {
        return notarizationData;
    }

    public String getCreatorAddr() {
        return creatorAddr;
    }

    public String getTxId() {
        return txId;
    }
}
