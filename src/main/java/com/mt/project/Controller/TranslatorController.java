package com.mt.project.Controller;

import com.mt.project.Service.TranslatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/translate")
public class TranslatorController {

    @Autowired
    private TranslatorService translatorService;

    @PostMapping
    public String translate(@RequestBody Map<String, String> request) {
        String text = request.get("description");
        return translatorService.translate(text);
    }

    @GetMapping("/test")
    public String test() {
        return translatorService.translate("Witaj świecie! To jest test tłumaczenia.");
    }

    @GetMapping("/test2")
    public String test2() {
        return translatorService.translate("Jak się masz?");
    }
}