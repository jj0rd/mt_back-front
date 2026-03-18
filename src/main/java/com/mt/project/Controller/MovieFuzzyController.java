package com.mt.project.Controller;

import com.mt.project.Dto.MovieFuzzyDiscoverRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class MovieFuzzyController {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/discover/fuzzy")
    public ResponseEntity<?> discoverFuzzy(@RequestBody MovieFuzzyDiscoverRequest request) {

        //  Szerokie zapytanie do discover (bez keywords i people)
        StringBuilder url = new StringBuilder(tmdbApiUrl + "/discover/movie?api_key=" + tmdbApiKey);
        url.append("&language=").append(request.getLanguage() != null ? request.getLanguage() : "pl-PL");
        url.append("&include_adult=false");

        if (request.getGenre() != null && !request.getGenre().isEmpty()) {
            String genres = request.getGenre().stream().map(String::valueOf).collect(Collectors.joining(","));
            url.append("&with_genres=").append(genres);
        }

        if (request.getYearFrom() != null) {
            url.append("&primary_release_date.gte=").append(request.getYearFrom()).append("-01-01");
        }
        if (request.getYearTo() != null) {
            url.append("&primary_release_date.lte=").append(request.getYearTo()).append("-12-31");
        }
        if (request.getRating() != null) {
            url.append("&vote_average.gte=").append(request.getRating());
        }

        Map<String, Object> result = restTemplate.getForObject(url.toString(), Map.class);
        List<Map<String, Object>> movies = (List<Map<String, Object>>) result.get("results");

        List<Map<String, Object>> rankedMovies = movies.stream().map(movie -> {
                    int score = 0;

                    // Gatunki
                    if (request.getGenre() != null && !request.getGenre().isEmpty()) {
                        List<Integer> movieGenres = (List<Integer>) movie.get("genre_ids");
                        score += movieGenres.stream().filter(request.getGenre()::contains).count();
                    }

                    // Pobierz szczegóły filmu z credits i keywords
                    String movieId = movie.get("id").toString();
                    String detailUrl = tmdbApiUrl + "/movie/" + movieId +
                            "?api_key=" + tmdbApiKey +
                            "&append_to_response=credits,keywords";
                    Map<String, Object> details = restTemplate.getForObject(detailUrl, Map.class);

                    // Keywords
                    if (request.getKeywords() != null && !request.getKeywords().isEmpty() && details.containsKey("keywords")) {
                        Map<String, Object> keywordsMap = (Map<String, Object>) details.get("keywords");
                        List<Map<String, Object>> movieKeywords = (List<Map<String, Object>>) keywordsMap.get("keywords");

                        score += movieKeywords.stream()
                                .map(k -> (Integer) k.get("id"))
                                .filter(request.getKeywords()::contains)
                                .count();

                        movie.put("keywordIds", movieKeywords.stream().map(k -> (Integer) k.get("id")).toList());
                    }

                    // People (cast + crew)
                    if (request.getPeople() != null && !request.getPeople().isEmpty() && details.containsKey("credits")) {
                        Map<String, Object> credits = (Map<String, Object>) details.get("credits");

                        List<Integer> castIds = ((List<Map<String, Object>>) credits.get("cast"))
                                .stream().map(c -> (Integer) c.get("id")).toList();
                        List<Integer> crewIds = ((List<Map<String, Object>>) credits.get("crew"))
                                .stream().map(c -> (Integer) c.get("id")).toList();

                        for (Integer id : request.getPeople()) {
                            if (castIds.contains(id) || crewIds.contains(id)) score++;
                        }
                        List<Integer> matchedPeopleIds = request.getPeople().stream()
                                .filter(id -> castIds.contains(id) || crewIds.contains(id))
                                .toList();
                        movie.put("peopleIds", matchedPeopleIds);
                    }

                    // Rok (dla pewności)
                    if (request.getYearFrom() != null && request.getYearTo() != null && movie.get("release_date") != null) {
                        int year = Integer.parseInt(((String) movie.get("release_date")).substring(0, 4));
                        if (year >= request.getYearFrom() && year <= request.getYearTo()) score++;
                    }

                    // Rating
                    if (request.getRating() != null && movie.get("vote_average") != null) {
                        Double voteAverage = ((Number) movie.get("vote_average")).doubleValue();
                        if (voteAverage >= request.getRating()) score++;
                    }

                    movie.put("score", score);
                    return movie;
                })
                .sorted((m1, m2) -> Integer.compare((Integer) m2.get("score"), (Integer) m1.get("score")))
                .toList();

        return ResponseEntity.ok(Map.of(
                "query", request,
                "totalResults", rankedMovies.size(),
                "movies", rankedMovies
        ));
    }

}
