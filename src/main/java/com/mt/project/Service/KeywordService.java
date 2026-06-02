package com.mt.project.Service;

import com.mt.project.Dto.KeywordSearchResponse;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class KeywordService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Autowired
    private POSTaggerME posTagger;

    public List<Integer> extractKeywordIds(String description) {

        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(description);
        String[] tags = posTagger.tag(tokens);

        Set<String> candidateKeywords = new HashSet<>();
        for (int i = 0; i < tokens.length; i++) {
            if (tags[i].startsWith("NN") && tokens[i].length() > 2) {
                candidateKeywords.add(tokens[i].toLowerCase());
            }
        }

        return candidateKeywords.stream()
                .limit(5) // 🔥 ważne (optymalizacja)
                .map(token -> {
                    try {
                        String url = tmdbApiUrl + "/search/keyword?api_key=" + tmdbApiKey +
                                "&query=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

                        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
                        List<Map<String, Object>> results =
                                (List<Map<String, Object>>) result.get("results");

                        if (results != null && !results.isEmpty()) {
                            return (Integer) results.get(0).get("id");
                        }
                    } catch (Exception e) {
                        System.out.println("Keyword error: " + token);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public KeywordSearchResponse searchKeyword(String keywordName) {

        if (keywordName == null || keywordName.trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword name cannot be empty");
        }

        String encodedName =
                URLEncoder.encode(keywordName.trim(), StandardCharsets.UTF_8);

        String url =
                tmdbApiUrl +
                        "/search/keyword?api_key=" +
                        tmdbApiKey +
                        "&query=" +
                        encodedName;

        Map<String, Object> result =
                restTemplate.getForObject(url, Map.class);

        if (result == null || !result.containsKey("results")) {
            throw new RuntimeException("Keyword not found");
        }

        List<Map<String, Object>> keywords =
                (List<Map<String, Object>>) result.get("results");

        if (keywords.isEmpty()) {
            throw new RuntimeException("Keyword not found");
        }

        Map<String, Object> keyword = keywords.get(0);

        KeywordSearchResponse response =
                new KeywordSearchResponse();

        response.setId((Integer) keyword.get("id"));
        response.setName((String) keyword.get("name"));

        return response;
    }

    public Map<String, Object> extractKeywordsWithNames(
            String description) {

        SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
        String[] tokens = tokenizer.tokenize(description);

        String[] tags = posTagger.tag(tokens);

        Set<String> candidateKeywords = new HashSet<>();

        for (int i = 0; i < tokens.length; i++) {

            if (tags[i].startsWith("NN")
                    && tokens[i].length() > 2) {

                candidateKeywords.add(
                        tokens[i].toLowerCase()
                );
            }
        }

        List<Map<String, Object>> foundKeywords =
                new ArrayList<>();

        for (String token : candidateKeywords) {

            try {

                String url =
                        tmdbApiUrl +
                                "/search/keyword?api_key=" +
                                tmdbApiKey +
                                "&query=" +
                                URLEncoder.encode(
                                        token,
                                        StandardCharsets.UTF_8
                                );

                Map<String, Object> result =
                        restTemplate.getForObject(
                                url,
                                Map.class
                        );

                List<Map<String, Object>> results =
                        (List<Map<String, Object>>)
                                result.get("results");

                if (results != null && !results.isEmpty()) {

                    Map<String, Object> keyword =
                            results.get(0);

                    foundKeywords.add(
                            Map.of(
                                    "id", keyword.get("id"),
                                    "name", keyword.get("name")
                            )
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "Keyword not found or error: "
                                + token
                );
            }
        }

        return Map.of(
                "description", description,
                "keywords", foundKeywords
        );
    }
}
