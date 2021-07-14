package it.davidlab.algonot.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.davidlab.algonot.dom.NotarizationCert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class StoreService {

    private final Logger logger = LoggerFactory.getLogger(StoreService.class);

    public void createPacket (byte[] docBytes, NotarizationCert cert) {

        String zipName = cert.getBlockchainData().getPacketName() + ".zip";
        String certName = "certificate.json";
        try (FileOutputStream fos = new FileOutputStream(zipName);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            ZipEntry zipEntryDoc = new ZipEntry(cert.getOriginalFileName());
            zipOut.putNextEntry(zipEntryDoc);

            zipOut.write(docBytes, 0, docBytes.length);

            ZipEntry zipEntryCert = new ZipEntry(certName);
            zipOut.putNextEntry(zipEntryCert);

            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
            byte[] certBytes = gson.toJson(cert).getBytes(StandardCharsets.UTF_8);
            zipOut.write(certBytes, 0, certBytes.length);

        }
        catch (IOException ex) {
            logger.error("Algorand ZIP creation Exception", ex);
        }

    }


}
