package com.mt.project.Controller;

import com.mt.project.Dto.KeywordSearchRequest;
import com.mt.project.Dto.KeywordSearchResponse;
import com.mt.project.Service.KeywordService;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/keyword")
public class KeywordController {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Autowired
    private RestTemplate restTemplate;

    private final KeywordService keywordService;

    public KeywordController(KeywordService keywordService) {
        this.keywordService = keywordService;
    }
    @PostMapping("/search")
    public ResponseEntity<?> searchKeyword(
            @RequestBody KeywordSearchRequest request) {

        try {

            return ResponseEntity.ok(
                    keywordService.searchKeyword(
                            request.getName()
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));

        } catch (RuntimeException e) {

            return ResponseEntity.status(404)
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    @PostMapping("/from-description")
    public ResponseEntity<?> extractKeywords(
            @RequestBody Map<String, String> request) {

        String description =
                request.get("description");

        if (description == null
                || description.trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Description cannot be empty"
                    ));
        }

        return ResponseEntity.ok(
                keywordService.extractKeywordsWithNames(
                        description
                )
        );
    }
}
