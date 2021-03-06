package it.davidlab.algonot.controller;

import com.google.gson.Gson;
import it.davidlab.algonot.domain.NotarizationCert;
import it.davidlab.algonot.dto.VerificationData;
import it.davidlab.algonot.exception.ApiException;
import it.davidlab.algonot.service.AlgorandService;
import it.davidlab.algonot.service.StoreService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class WebController {

    private final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Value("${algorand.explorer.url}")
    private String EXPLORER_URL;

    private final AlgorandService algorandService;
    private final StoreService storeService;

    public WebController(AlgorandService algorandService, StoreService storeService) {
        this.algorandService = algorandService;
        this.storeService = storeService;
    }

    @GetMapping("/")
    public String main() {
        return "index";
    }


    @PostMapping("notarize")
    public String notarizeReq(@RequestParam("file") MultipartFile file,
                              @RequestParam String note, Model model) {

        boolean notarizationSuccess = true;
        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand file reading Exception", e);
            model.addAttribute("valid", false);
            model.addAttribute("message", e.getMessage());
            return "notarization-result";
        }

        // check if document is not empty
        if (docBytes.length == 0) {
            logger.error("Error: File cannot be empty");
            model.addAttribute("valid", false);
            model.addAttribute("message", "File cannot be empty");
            return "notarization-result";
        }

        // notarize the document
        Optional<NotarizationCert> notarizationCertOpt
                = algorandService.notarize(file.getOriginalFilename(), note, file.getSize(), docBytes);

        if (notarizationCertOpt.isEmpty()) {
            model.addAttribute("valid", false);
            model.addAttribute("message", "Notarization error");
            return "notarization-result";
        }

        // create the Zip packet
        byte[] zipPacketBytes = storeService.createPacket(docBytes, notarizationCertOpt.get());

        // save the packet

        String zipFileName = notarizationCertOpt.get().getBlockchainData().getPacketCode()
                + "-" + Instant.now().getEpochSecond() + ".zip";
        File zipFile = new File(zipFileName);

        try {
            FileUtils.writeByteArrayToFile(zipFile, zipPacketBytes);
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            notarizationSuccess = false;
            model.addAttribute("message", "File cannot be empty");
        }

        if (notarizationSuccess) {
            model.addAttribute("explorerUrl", EXPLORER_URL);
            model.addAttribute("certInfo", notarizationCertOpt.get());
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
            model.addAttribute("valid", false);
            model.addAttribute("message", "Cannot read the file");
            return "verification-result";
        }

        Optional<NotarizationCert> certOptional =  verifyPacket(docBytes);

        certOptional.ifPresentOrElse(
                cert -> {
                    model.addAttribute("certInfo",cert);
                    model.addAttribute("valid", true);
                    model.addAttribute("explorerUrl", EXPLORER_URL);},
                () -> {
                    model.addAttribute("message", "Certification failed");
                    model.addAttribute("valid", false);
                });

        return "verification-result";
    }


    @PostMapping(value = "/api/notarize", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> notarizizationApi(@RequestParam("file") MultipartFile file,
                                                    @RequestParam("note") String note) {

        byte[] zipPacket = {};
        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            return ResponseEntity.badRequest().body(zipPacket);
        }

        // notarize the document
        Optional<NotarizationCert> notarizationCertOpt
                = algorandService.notarize(file.getOriginalFilename(), note, file.getSize(), docBytes);


        if (notarizationCertOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(zipPacket);
        }

        // create the Zip packet
        zipPacket =  storeService.createPacket(docBytes, notarizationCertOpt.get());

        String zipFileName = notarizationCertOpt.get().getBlockchainData().getPacketCode()
                + "-" + Instant.now().getEpochSecond() + ".zip";
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
            throw new ApiException();
        }

        Optional<NotarizationCert> certOptional =  verifyPacket(docBytes);

        if (certOptional.isPresent()) {
            return certOptional.get();
        }
        else {
            throw new ApiException();
        }

    }


    public Optional<NotarizationCert> verifyPacket(byte[] sourcePacket) {

        Map<String, byte[]> packetItems = new HashMap<>(5);

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
        NotarizationCert packetCertificate = new Gson().fromJson(certReader, NotarizationCert.class);


        //check if the certified document exists in the zip
        if (!packetItems.containsKey(packetCertificate.getOriginalFileName())) {
            logger.error("Wrong certificate error");
            return Optional.empty();
        }

        // Conditions to verify:
        // (1) <packet document hash> == <certificate document hash> == <blockchain document hash>
        // (2) <certificate creator address> == <blockchain sender address>
        // (3) <certificate block> == <blockchain block>


        // get data from the specified Algorand tx
        VerificationData verificationData;
        try {
            verificationData = algorandService.getDataFromTx(packetCertificate.getTxId());
        }
        catch (Exception e) {
            logger.error("Cannot retrieve certificate from the blockchain");
            return Optional.empty();
        }

        // Verify condition (1)
        byte[] documentContent = packetItems.get(packetCertificate.getOriginalFileName());
        String packetDocHash = DigestUtils.sha256Hex(documentContent);
        String certificateDocHash = packetCertificate.getBlockchainData().getDocumentHash();
        String blockchainDocHash = verificationData.getDocumentHash();

        boolean verify1 = packetDocHash.equals(certificateDocHash)
                && certificateDocHash.equals(blockchainDocHash);

        // Verify condition (2)
        boolean verify2 = packetCertificate.getCreatorAddr().equals(verificationData.getSenderAddr());

        // Verify condition (3)
        boolean verify3 = packetCertificate.getBlockNum() == verificationData.getBlockNum();

        boolean verified = verify1 && verify2 && verify3;

        // return the certificate or an empty optional if the sourcePacket is not verified
        return verified? Optional.of(packetCertificate):Optional.empty();
    }




}
