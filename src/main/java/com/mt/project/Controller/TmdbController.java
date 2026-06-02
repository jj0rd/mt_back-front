package com.mt.project.Controller;

import com.mt.project.Dto.MovieDiscoverRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tmdb/movies")
public class TmdbController {

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    private final RestTemplate restTemplate;

    public TmdbController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/search/{title}")
    public ResponseEntity<?> searchMovie(@PathVariable String title,@RequestParam(value = "lang", defaultValue = "en-US") String lang) {
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

        Map<Integer, Map<String, Object>> merged = new HashMap<>();

        // =========================
        // 1. GENRE QUERY
        // =========================
        if (request.getGenre() != null && !request.getGenre().isEmpty()) {

            String genres = join(request.getGenre());

            String url = tmdbApiUrl + "/discover/movie"
                    + "?api_key=" + tmdbApiKey
                    + "&with_genres=" + genres;

            merge(merged, fetch(url), "genre");
        }

        // =========================
        // 2. CAST / PEOPLE QUERY
        // =========================
        if (request.getPeople() != null && !request.getPeople().isEmpty()) {

            String people = join(request.getPeople());

            String url = tmdbApiUrl + "/discover/movie"
                    + "?api_key=" + tmdbApiKey
                    + "&with_people=" + people;

            merge(merged, fetch(url), "people");
        }

        // =========================
        // 3. KEYWORDS QUERY
        // =========================
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {

            String keywords = join(request.getKeywords());

            String url = tmdbApiUrl + "/discover/movie"
                    + "?api_key=" + tmdbApiKey
                    + "&with_keywords=" + keywords;

            merge(merged, fetch(url), "keywords");
        }

        // =========================
        // 4. YEAR RANGE QUERY
        // =========================
        if (request.getYearFrom() != null || request.getYearTo() != null) {

            StringBuilder url = new StringBuilder(tmdbApiUrl + "/discover/movie?api_key=" + tmdbApiKey);

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

            merge(merged, fetch(url.toString()), "year");
        }

        // =========================
        // 5. FINAL FILTER (USER PARAMETERS)
        // =========================
        List<Map<String, Object>> result = merged.values().stream()
                .filter(m -> filterByRating(m, request))
                .toList();

        return ResponseEntity.ok(result);
    }

    private void merge(
            Map<Integer, Map<String, Object>> merged,
            List<Map<String, Object>> movies,
            String source
    ) {

        for (Map<String, Object> m : movies) {

            Integer id = (Integer) m.get("id");
            if (id == null) continue;

            if (!merged.containsKey(id)) {

                m.put("sources", new HashSet<String>());
                ((Set<String>) m.get("sources")).add(source);

                merged.put(id, m);

            } else {
                ((Set<String>) merged.get(id).get("sources")).add(source);
            }
        }
    }
    private List<Map<String, Object>> fetch(String url) {

        Map<String, Object> res =
                restTemplate.getForObject(url, Map.class);

        if (res == null || !res.containsKey("results")) {
            return Collections.emptyList();
        }

        return (List<Map<String, Object>>) res.get("results");
    }
    private boolean filterByRating(Map<String, Object> movie, MovieDiscoverRequest request) {

        if (request.getRating() == null) {
            return true;
        }

        Double rating =
                ((Number) movie.getOrDefault("vote_average", 0)).doubleValue();

        return rating >= request.getRating();
    }
    private String join(List<?> list) {
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("|"));
    }
    @GetMapping("/genres")
    public ResponseEntity<?> getGenres() {

        String url = tmdbApiUrl + "/genre/movie/list?api_key=" + tmdbApiKey + "&language=en-US";

        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        return ResponseEntity.ok(result);
    }
}