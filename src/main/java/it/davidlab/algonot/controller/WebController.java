package it.davidlab.algonot.controller;

import com.google.gson.Gson;
import it.davidlab.algonot.dom.BlockchainData;
import it.davidlab.algonot.dom.NotarizationCert;
import it.davidlab.algonot.exception.ApiException;
import it.davidlab.algonot.service.BlockchainServiceInterface;
import it.davidlab.algonot.service.StoreServiceInterface;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class WebController {

    private final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Value("${algorand.account.address}")
    private String ACC_ADDRESS;

    @Value("${algorand.explorer.url}")
    private String EXPLORER_URL;

    private final BlockchainServiceInterface algorandService;
    private final StoreServiceInterface storeService;

    public WebController(BlockchainServiceInterface algorandService, StoreServiceInterface storeService) {
        this.algorandService = algorandService;
        this.storeService = storeService;
    }

    @GetMapping("/")
    public String main() {
        return "index";
    }


    @PostMapping("notarize")
    public String notarizeReq(@RequestParam("file") MultipartFile file, Model model) {

        boolean notarizationSuccess = true;
        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand file reading Exception", e);
            model.addAttribute("valid", false);
            return "notarization-result";
        }

        // check if document is not empty
        if (docBytes.length == 0) {
            logger.error("Error: File cannot be empty");
            model.addAttribute("valid", false);
            return "notarization-result";
        }

        // notarize the document
        NotarizationCert notarizationCert
                = algorandService.notarize(file.getOriginalFilename(), file.getSize(), docBytes);

        // create the Zip packet
        byte[] zipPacketBytes = storeService.createPacket(docBytes, notarizationCert);
        String zipFileName = notarizationCert.getBlockchainData().getPacketName() + ".zip";

        File zipFile = new File(zipFileName);

        try {
            FileUtils.writeByteArrayToFile(zipFile, zipPacketBytes);
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            notarizationSuccess = false;
        }

        if (notarizationSuccess) {
            model.addAttribute("explorerUrl", EXPLORER_URL);
            model.addAttribute("certInfo", notarizationCert);
            model.addAttribute("valid", true);
        }
        else {
            model.addAttribute("valid", false);
        }

        return "notarization-result";
    }


    @PostMapping("verify")
    public String verifyReq(@RequestParam("file") MultipartFile file, Model model) {

        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            throw new ApiException("Client Exception");
        }

        Optional<NotarizationCert> certOptional =  verify(docBytes);

        certOptional.ifPresentOrElse(
                cert -> {
                    model.addAttribute("certInfo",cert);
                    model.addAttribute("valid", true);
                    model.addAttribute("explorerUrl", EXPLORER_URL);},
                () -> {
                    model.addAttribute("valid", false);
                });

        return "verification-result";
    }


    public Optional<NotarizationCert> verify(byte[] sourcePacket) {

        Map<String, byte[]> packetItems = new HashMap<>();

        //extract items from the zip source Packet
        try (ByteArrayInputStream zipIS = new ByteArrayInputStream(sourcePacket);
             ZipInputStream zis = new ZipInputStream(zipIS)) {

            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    packetItems.put(zipEntry.getName(), zis.readAllBytes());
                }

                zipEntry = zis.getNextEntry();
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }

        // check if 'certificate.json' exists
        if (!packetItems.containsKey("certificate.json")) {
            logger.error("Wrong sourcePacket");
            return Optional.empty();
        }

        // convert the Json Certificate in a Certificate Object
        Reader certReader = new InputStreamReader(new ByteArrayInputStream(packetItems.get("certificate.json"))
                , StandardCharsets.UTF_8);
        NotarizationCert sourceCertificate = new Gson().fromJson(certReader, NotarizationCert.class);


        // verify if the certificate is not emply
        if (sourceCertificate.checkNull()) {
            logger.error("Empty certificate error");
            return Optional.empty();
        }

        //
        if (!packetItems.containsKey(sourceCertificate.getOriginalFileName())) {
            logger.error("Wrong certificate error");
            return Optional.empty();
        }

        // Conditions we need to verify
        // (1) source computed document hash == blockchain-data document hash == certificate-data document hash
        // (2) source computed packetName == blockchain-data sourcePacket name == certificate-data sourcePacket name
        // (3) (Extra Optional) source doc Size == blockchain-data docSize == certificate-data docSize
        // (4) (Extra Optional) source doc Name == blockchain-data docName == certificate-data docName

        // get data from the specified tx
        BlockchainData blockchainCert = algorandService.getTxData(sourceCertificate.getTxId());

        // verify condition (1)
        byte[] documentContent = packetItems.get(sourceCertificate.getOriginalFileName());
        String sourceDocHash = DigestUtils.sha256Hex(documentContent);
        String sourceCertDocHash = sourceCertificate.getBlockchainData().getDocumentHash();
        String blockchainDocHash = blockchainCert.getDocumentHash();

        boolean verify1 = sourceDocHash.equals(sourceCertDocHash)
                && sourceCertDocHash.equals(blockchainDocHash);

        // verify condition (2)
        String sourcePaketCode = ACC_ADDRESS + sourceDocHash;
        String sourcePacketName = DigestUtils.sha256Hex(sourcePaketCode.getBytes(StandardCharsets.UTF_8));
        String sourceCertPacketName = sourceCertificate.getBlockchainData().getPacketName();
        String blockchainPacketName = blockchainCert.getPacketName();

        boolean verify2 = sourcePacketName.equals(sourceCertPacketName)
                && sourcePacketName.equals(blockchainPacketName);


        // verify if the info in the certificate are the same stored on the Blockchain
        boolean verified = verify1 && verify2;

        // return the certificate or an empty optional if the sourcePacket is not verified
        return verified? Optional.of(sourceCertificate):Optional.empty();
    }


    @PostMapping(value = "/api/notarize", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> notarizizationApi(@RequestParam("file") MultipartFile file) {

        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            throw new ApiException("Algorand Client creation Exception");
        }

        // notarize the document
        NotarizationCert notarizationCert
                = algorandService.notarize(file.getOriginalFilename(), file.getSize(), docBytes);

        // create the Zip packet
        byte[] zipPacket =  storeService.createPacket(docBytes, notarizationCert);

        String zipFileName = notarizationCert.getBlockchainData().getPacketName() + ".zip";
        return ResponseEntity.ok()
                .contentLength(zipPacket.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"")
                .body(zipPacket);
    }



    @PostMapping(value = "/api/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public NotarizationCert verifyApi(@RequestParam("file") MultipartFile file) {

        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            throw new ApiException("Client Exception");
        }

        Optional<NotarizationCert> certOptional =  verify(docBytes);

        if (certOptional.isPresent()) {
            return certOptional.get();
        }
        else {
            throw new ApiException("Verification Error");
        }

    }


    // Exceptions

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> VerificationException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }


}
