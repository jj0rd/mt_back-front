package com.mt.project.Service;

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
}
