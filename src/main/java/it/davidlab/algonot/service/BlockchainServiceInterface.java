package it.davidlab.algonot.service;

import it.davidlab.algonot.dom.BlockchainData;
import it.davidlab.algonot.dom.NotarizationCert;

import java.util.Optional;

public interface BlockchainServiceInterface {
    NotarizationCert notarize(String docName, long docSize, byte[] docBytes);

    BlockchainData getTxData (String txId);

    void waitForConfirmation(String txId, int timeout) throws Exception;
}
