package com.mt.project.controllerTest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/omdb/movies")
public class OmdbController {

    @Value("${omdb.api.key}")
    private String apiKey;

    @Value("${omdb.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/{title}")
    public ResponseEntity<?> getMovie(@PathVariable String title) {
        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("Error", "Title cannot be empty"));
            }

            String url = apiUrl + "?apikey=" + apiKey + "&t=" + title.trim();
            Map<String, Object> movie = restTemplate.getForObject(url, Map.class);

            if (movie == null || !"True".equals(movie.get("Response"))) {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("Error", "Movie not found!"));
            }

            return ResponseEntity.ok(movie);

        } catch (RestClientException e) {
            return ResponseEntity.status(503)
                    .body(Collections.singletonMap("Error", "OMDb service unavailable"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("Error", "Internal server error"));
        }
    }
}
