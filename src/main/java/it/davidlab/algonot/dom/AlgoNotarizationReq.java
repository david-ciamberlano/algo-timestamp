package it.davidlab.algonot.dom;

import java.util.Date;

public class AlgoNotarizationReq {

    private final String sha256hexContent;
    private final String documentName;
    private final long docSize;
    private final Date requestDate;


    public AlgoNotarizationReq(String sha256hexContent, String documentName, long docSize, Date requestDate) {
        this.sha256hexContent = sha256hexContent;
        this.documentName = documentName;
        this.docSize = docSize;
        this.requestDate = requestDate;
    }


    public String getSha256hexContent() {
        return this.sha256hexContent;
    }

    public String getDocumentName() {
        return this.documentName;
    }

    public long getDocSize() {
        return docSize;
    }

}
