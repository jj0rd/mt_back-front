package com.mt.project.Controller;

import com.mt.project.Dto.MovieFuzzyDiscoverRequest;
import com.mt.project.Service.KeywordService;
import com.mt.project.Service.PersonService;
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
    @Autowired
    private KeywordService keywordService;
    @Autowired
    private PersonService personService;

    @PostMapping("/discover/fuzzy")
    public ResponseEntity<?> discoverFuzzy(@RequestBody MovieFuzzyDiscoverRequest request) {

        //  Szerokie zapytanie do discover (bez keywords i people)
        StringBuilder url = new StringBuilder(tmdbApiUrl + "/discover/movie?api_key=" + tmdbApiKey);
        url.append("&language=").append(request.getLanguage() != null ? request.getLanguage() : "en-US");
        url.append("&include_adult=false");

        if (request.getGenre() != null && !request.getGenre().isEmpty()) {
            String genres = request.getGenre().stream().map(String::valueOf).collect(Collectors.joining("|"));
            url.append("&with_genres=").append(genres);
        }

//        if (request.getYearFrom() != null) {
//            url.append("&primary_release_date.gte=").append(request.getYearFrom()).append("-01-01");
//        }
//        if (request.getYearTo() != null) {
//            url.append("&primary_release_date.lte=").append(request.getYearTo()).append("-12-31");
//        }
//        if (request.getRating() != null) {
//            url.append("&vote_average.gte=").append(request.getRating());
//        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            List<Integer> extractedKeywordIds = keywordService.extractKeywordIds(request.getDescription());

            // merge z istniejącymi keywordami (jeśli ktoś poda ręcznie)
            Set<Integer> allKeywords = new HashSet<>();
            if (request.getKeywords() != null) {
                allKeywords.addAll(request.getKeywords());
            }
            allKeywords.addAll(extractedKeywordIds);

            request.setKeywords(new ArrayList<>(allKeywords));
        }
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            String keywords = request.getKeywords().stream().map(String::valueOf).collect(Collectors.joining("|"));
            url.append("&with_keywords=").append(keywords);
        }

        // 🔹 Zamiana nazwisk osób na ID
        if (request.getPeopleNames() != null && !request.getPeopleNames().isEmpty()) {
            List<Integer> peopleIds = personService.getPersonIdsByNames(request.getPeopleNames());
            request.setPeople(peopleIds);
        }



        // 🔹 Paginacja - pobieramy np. do 5 stron
        List<Map<String, Object>> allMovies = new ArrayList<>();
        int maxPages = 5;
        for (int page = 1; page <= maxPages; page++) {
            String pagedUrl = url.toString() + "&page=" + page;
            Map<String, Object> result = restTemplate.getForObject(pagedUrl, Map.class);
            if (result == null || !result.containsKey("results")) break;
            List<Map<String, Object>> moviesPage = (List<Map<String, Object>>) result.get("results");
            if (moviesPage.isEmpty()) break;
            allMovies.addAll(moviesPage);
        }

        // 🔹 Ranking filmów
        List<Map<String, Object>> rankedMovies = allMovies.stream().map(movie -> {
                    int score = 0;

                    // Gatunki
                    if (request.getGenre() != null && !request.getGenre().isEmpty()) {
                        List<Integer> movieGenres = (List<Integer>) movie.get("genre_ids");
                        score += movieGenres.stream().filter(request.getGenre()::contains).count();
                    }

                    // Szczegóły filmu: credits + keywords
                    String movieId = movie.get("id").toString();
                    String detailUrl = tmdbApiUrl + "/movie/" + movieId +
                            "?api_key=" + tmdbApiKey +
                            "&append_to_response=credits,keywords";
                    Map<String, Object> details = restTemplate.getForObject(detailUrl, Map.class);

                    // Keywordy
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

                    // Rok
                    if (request.getYearFrom() != null && request.getYearTo() != null) {
                        String releaseDate = (String) movie.get("release_date");
                        if (releaseDate != null && releaseDate.length() >= 4) {
                            int year = Integer.parseInt(releaseDate.substring(0, 4));
                            if (year >= request.getYearFrom() && year <= request.getYearTo()) score++;
                        }
                    }

                    // Rating
                    if (request.getRating() != null && movie.get("vote_average") != null) {
                        Double voteAverage = ((Number) movie.get("vote_average")).doubleValue();
                        if (voteAverage >= request.getRating()) score++;
                    }

                    movie.put("score", score);
                    return movie;
                }).sorted((m1, m2) -> Integer.compare((Integer) m2.get("score"), (Integer) m1.get("score")))
                .toList();

        return ResponseEntity.ok(Map.of(
                "query", request,
                "totalResults", rankedMovies.size(),
                "movies", rankedMovies
        ));
    }

}
