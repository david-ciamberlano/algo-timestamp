package it.davidlab.algonot.controller;

import it.davidlab.algonot.dom.NotarizationCert;
import it.davidlab.algonot.service.AlgorandService;
import it.davidlab.algonot.service.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Controller
public class WebController {

    private final Logger logger = LoggerFactory.getLogger(WebController.class);

    private final AlgorandService algorandService;
    private final StoreService storeService;

    public WebController(AlgorandService algorandService, StoreService storeService) {
        this.algorandService = algorandService;
        this.storeService = storeService;
    }

    @PostMapping("notarize")
    public String notarizization(@RequestParam("file") MultipartFile file, Model model) {

        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            throw new RuntimeException("Client Exception", e);
        }

        // notarize the document
        NotarizationCert notarizationCert
                = algorandService.notarize(file.getOriginalFilename(), file.getSize(), docBytes);

        // create the Zip packet
        storeService.createPacket(docBytes, notarizationCert);

        model.addAttribute("message","Notarization completed");

        return "notarization-result";
    }


    @PostMapping("verify")
    public String verify(@RequestParam("file") MultipartFile file, Model model) {

        byte[] docBytes;
        try {
            docBytes = file.getBytes();
        }
        catch (IOException e) {
            logger.error("Algorand Client creation Exception", e);
            throw new RuntimeException("Client Exception", e);
        }

        Optional<NotarizationCert> certOptional =  algorandService.verify(docBytes);

        certOptional.ifPresentOrElse(
                cert -> {
                    model.addAttribute("certInfo",cert);
                    model.addAttribute("valid", true);},
                () -> {
                    model.addAttribute("valid", false);
                });

        return "verification-result";
    }

}
