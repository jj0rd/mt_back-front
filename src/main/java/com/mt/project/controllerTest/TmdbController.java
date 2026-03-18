package com.mt.project.controllerTest;

import com.mt.project.Dto.MovieDiscoverRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tmdb/movies")
public class TmdbController {

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/search/{title}")
    public ResponseEntity<?> searchMovie(@PathVariable String title,@RequestParam(value = "lang", defaultValue = "pl-PL") String lang) {
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("Error", "Title cannot be empty"));
        }

        String url = tmdbApiUrl + "/search/movie?api_key=" + tmdbApiKey +
                "&query=" + title.trim() +
                "&language=" + lang;
        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        if (result == null || !result.containsKey("results")) {
            return ResponseEntity.status(404)
                    .body(Collections.singletonMap("Error", "Movie not found!"));
        }

        List<Map<String, Object>> movies = (List<Map<String, Object>>) result.get("results");
        return ResponseEntity.ok(Map.of(
                "query", title,
                "language", lang,
                "totalResults", movies.size(),
                "movies", movies
        ));
    }
    @PostMapping("/discover")
    public ResponseEntity<?> discoverMovies(@RequestBody MovieDiscoverRequest request) {

        StringBuilder url = new StringBuilder(tmdbApiUrl + "/discover/movie?api_key=" + tmdbApiKey);

        url.append("&language=").append(
                request.getLanguage() != null ? request.getLanguage() : "pl-PL"
        );

        if (request.getGenre() != null && !request.getGenre().isEmpty()) {

            String genres = request.getGenre()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            url.append("&with_genres=").append(genres);
        }

        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {

            String keywordParam = request.getKeywords()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")); // lub "|" dla OR

            url.append("&with_keywords=").append(keywordParam);
        }

        if (request.getPeople() != null && !request.getPeople().isEmpty()) {

            String people = request.getPeople()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            url.append("&with_people=").append(people);
        }

        if (request.getYearFrom() != null) {
            url.append("&primary_release_date.gte=")
                    .append(request.getYearFrom())
                    .append("-01-01");
        }

        if (request.getYearTo() != null) {
            url.append("&primary_release_date.lte=")
                    .append(request.getYearTo())
                    .append("-12-31");
        }

        if (request.getRating() != null) {
            url.append("&vote_average.gte=").append(request.getRating());
        }

        Map<String, Object> result = restTemplate.getForObject(url.toString(), Map.class);

        return ResponseEntity.ok(result);
    }
    @GetMapping("/genres")
    public ResponseEntity<?> getGenres() {

        String url = tmdbApiUrl + "/genre/movie/list?api_key=" + tmdbApiKey + "&language=pl-PL";

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        return ResponseEntity.ok(result);
    }
}