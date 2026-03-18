package com.mt.project.Controller;

import com.mt.project.Dto.KeywordSearchRequest;
import com.mt.project.Dto.KeywordSearchResponse;
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

    private POSTaggerME posTagger;

    public KeywordController() throws Exception {
        // Ładujemy model POS z OpenNLP (rzeczowniki, czasowniki, etc.)
        try (InputStream modelStream = getClass().getResourceAsStream("/models/en-pos-maxent.bin")) {
            POSModel model = new POSModel(modelStream);
            posTagger = new POSTaggerME(model);
        }
    }
    @PostMapping("/search")
    public ResponseEntity<?> searchKeyword(@RequestBody KeywordSearchRequest request) {

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Keyword name cannot be empty"));
        }

        String encodedName = URLEncoder.encode(request.getName().trim(), StandardCharsets.UTF_8);

        String url = tmdbApiUrl + "/search/keyword?api_key=" + tmdbApiKey + "&query=" + encodedName;

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        if (result == null || !result.containsKey("results")) {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("error", "Keyword not found"));
        }

        List<Map<String, Object>> keywords = (List<Map<String, Object>>) result.get("results");

        if (keywords.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("error", "Keyword not found"));
        }

        // dla uproszczenia zwracamy pierwsze pasujące słowo kluczowe
        Map<String, Object> keyword = keywords.get(0);

        KeywordSearchResponse response = new KeywordSearchResponse();
        response.setId((Integer) keyword.get("id"));
        response.setName((String) keyword.get("name"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/from-description")
    public ResponseEntity<?> extractKeywords(@RequestBody Map<String, String> request) {

        String description = request.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Description cannot be empty"));
        }

        //  Tokenizacja
        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(description);

        //  POS Tagging
        String[] tags = posTagger.tag(tokens);

        //  Wybieramy tylko rzeczowniki i ważne frazy (NN, NNP, NNS)
        Set<String> candidateKeywords = new HashSet<>();
        for (int i = 0; i < tokens.length; i++) {
            String tag = tags[i];
            if (tag.startsWith("NN") && tokens[i].length() > 2) {
                candidateKeywords.add(tokens[i].toLowerCase());
            }
        }

        List<Map<String, Object>> foundKeywords = new ArrayList<>();

        //  Szukamy tokenów w TMDB
        for (String token : candidateKeywords) {
            try {
                String url = tmdbApiUrl + "/search/keyword?api_key=" + tmdbApiKey + "&query=" +
                        URLEncoder.encode(token, StandardCharsets.UTF_8);
                Map<String, Object> result = restTemplate.getForObject(url, Map.class);

                List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> keyword = results.get(0);
                    foundKeywords.add(Map.of(
                            "id", keyword.get("id"),
                            "name", keyword.get("name")
                    ));
                }
            } catch (Exception e) {
                // logujemy nieudane zapytania
                System.out.println("Keyword not found or error: " + token);
            }
        }

        return ResponseEntity.ok(Map.of(
                "description", description,
                "keywords", foundKeywords
        ));
    }
}
