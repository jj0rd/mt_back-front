package com.mt.project.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PersonService {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private final RestTemplate restTemplate;

    public PersonService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Integer getPersonIdByName(String name) {
        if (name == null || name.isBlank()) return null;
        String encodedName = URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        String url = tmdbApiUrl + "/search/person?api_key=" + tmdbApiKey + "&query=" + encodedName;

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
        if (result == null || !result.containsKey("results")) return null;

        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        if (results.isEmpty()) return null;

        return (Integer) results.get(0).get("id"); // pierwsza osoba w wynikach
    }

    public List<Integer> getPersonIdsByNames(List<String> names) {
        return names.stream()
                .map(this::getPersonIdByName)
                .filter(Objects::nonNull)
                .toList();
    }
}
