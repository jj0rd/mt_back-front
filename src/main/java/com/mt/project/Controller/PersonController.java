package com.mt.project.Controller;

import com.mt.project.Dto.PersonSearchRequest;
import com.mt.project.Dto.PersonSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/person")
public class PersonController {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Autowired
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/search")
    public ResponseEntity<?> searchPerson(@RequestBody PersonSearchRequest request) {

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Name cannot be empty"));
        }

        String encodedName = URLEncoder.encode(request.getName().trim(), StandardCharsets.UTF_8);

        String url = tmdbApiUrl + "/search/person?api_key=" + tmdbApiKey +
                "&query=" + encodedName;

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        if (result == null || !result.containsKey("results")) {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("error", "Person not found"));
        }

        List<Map<String, Object>> people = (List<Map<String, Object>>) result.get("results");

        // dla uproszczenia zwracamy pierwszą osobę w wynikach
        if (people.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("error", "Person not found"));
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

        return ResponseEntity.ok(response);
    }
}
