package com.mt.project.Service;

import com.mt.project.Dto.PersonSearchResponse;
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

    public PersonSearchResponse searchPerson(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        String encodedName = URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);

        String url = tmdbApiUrl + "/search/person?api_key=" + tmdbApiKey +
                "&query=" + encodedName;

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        if (result == null || !result.containsKey("results")) {
            throw new RuntimeException("Person not found");
        }

        List<Map<String, Object>> people =
                (List<Map<String, Object>>) result.get("results");

        if (people.isEmpty()) {
            throw new RuntimeException("Person not found");
        }

        Map<String, Object> person = people.get(0);

        PersonSearchResponse response = new PersonSearchResponse();
        response.setId((Integer) person.get("id"));
        response.setName((String) person.get("name"));

        List<String> knownFor = ((List<Map<String, Object>>) person.get("known_for"))
                .stream()
                .map(kf -> (String) kf.get("title"))
                .toList();

        response.setKnownFor(knownFor);

        return response;
    }
}
