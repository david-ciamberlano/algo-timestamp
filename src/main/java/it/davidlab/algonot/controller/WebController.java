package it.davidlab.algonot.controller;

import it.davidlab.algonot.service.AlgorandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class WebController {

    private final Logger logger = LoggerFactory.getLogger(WebController.class);

    private AlgorandService algorandService;

    public WebController(AlgorandService algorandService) {
        this.algorandService = algorandService;
    }

    @GetMapping("/algonot")
    public String index(Model model) {

        return "index";
    }

    @PostMapping("/notarize")
    public String notarizization(@RequestParam("file") MultipartFile file, Model model) {

        algorandService.notarize(file);

        return "index";
    }



}
