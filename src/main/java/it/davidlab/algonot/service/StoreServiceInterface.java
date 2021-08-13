package it.davidlab.algonot.service;

import it.davidlab.algonot.dom.NotarizationCert;

public interface StoreServiceInterface {
    byte[] createPacket(byte[] docBytes, NotarizationCert cert);
}
