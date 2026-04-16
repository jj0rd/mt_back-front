package com.mt.project.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 🔹 Zamiana tytułu filmu na TMDb ID
    public Integer findMovieIdByTitle(String title) {
        try {
            String encoded = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
            String url = tmdbApiUrl + "/search/movie?api_key=" + tmdbApiKey + "&query=" + encoded;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("results")) return null;

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results.isEmpty()) return null;

            return (Integer) results.get(0).get("id");
        } catch (Exception e) {
            return null;
        }
    }

    // 🔹 Pobranie szczegółów filmu (gatunki, keywordy, obsada)
    private Map<String, Object> getMovieDetails(Integer movieId) {
        try {
            String detailUrl = tmdbApiUrl + "/movie/" + movieId
                    + "?api_key=" + tmdbApiKey + "&append_to_response=credits,keywords";
            Map<String, Object> details = restTemplate.getForObject(detailUrl, Map.class);

            if (details != null && details.containsKey("keywords")) {
                Map<String, Object> keywordsMap = (Map<String, Object>) details.get("keywords");
                List<Map<String, Object>> movieKeywords = (List<Map<String, Object>>) keywordsMap.get("keywords");
                details.put("keywordIds", movieKeywords.stream().map(k -> (Integer) k.get("id")).toList());
            }

            if (details != null && details.containsKey("genres")) {
                List<Map<String, Object>> genres = (List<Map<String, Object>>) details.get("genres");
                details.put("genre_ids", genres.stream().map(g -> (Integer) g.get("id")).toList());
            }

            return details;
        } catch (Exception e) {
            return null;
        }
    }

    // 🔹 Główna metoda rekomendacji
    public List<Map<String, Object>> recommendMovies(List<String> movieTitles) {
        // 1️⃣ Zamień tytuły → ID
        List<Integer> movieIds = movieTitles.stream()
                .map(this::findMovieIdByTitle)
                .filter(Objects::nonNull)
                .toList();
        if (movieIds.isEmpty()) return Collections.emptyList();

        // 2️⃣ Pobierz szczegóły filmów wejściowych
        List<Map<String, Object>> inputMoviesDetails = movieIds.stream()
                .map(this::getMovieDetails)
                .filter(Objects::nonNull)
                .toList();

        // 3️⃣ Wyciągnij wszystkie gatunki i keywordy z filmów wejściowych
        Set<Integer> allGenres = new HashSet<>();
        Set<Integer> allKeywords = new HashSet<>();
        for (Map<String, Object> m : inputMoviesDetails) {
            allGenres.addAll((List<Integer>) m.getOrDefault("genre_ids", Collections.emptyList()));
            allKeywords.addAll((List<Integer>) m.getOrDefault("keywordIds", Collections.emptyList()));
        }

        // 4️⃣ Pobierz filmy z discover z filtrem po gatunkach i keywordach
        StringBuilder url = new StringBuilder(tmdbApiUrl + "/discover/movie?api_key=" + tmdbApiKey
                + "&language=en-US&include_adult=false");

        if (!allGenres.isEmpty()) {
            url.append("&with_genres=").append(allGenres.stream().map(String::valueOf).collect(Collectors.joining("|")));
        }
        if (!allKeywords.isEmpty()) {
            url.append("&with_keywords=").append(allKeywords.stream().map(String::valueOf).collect(Collectors.joining("|")));
        }

        // 🔹 Paginacja
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

        // 6️⃣ ID wejściowe (set dla szybkiego filtrowania)
        Set<Integer> inputIds = new HashSet<>(movieIds);

        // 7️⃣ Ranking
        List<Map<String, Object>> rankedMovies = allMovies.stream()
                .filter(m -> !inputIds.contains((Integer) m.get("id")))
                .map(movie -> {

                    Map<String, Object> details = getMovieDetails((Integer) movie.get("id"));
                    if (details == null) return null;

                    int score = 0;

                    List<Integer> movieGenres =
                            (List<Integer>) details.getOrDefault("genre_ids", Collections.emptyList());

                    List<Integer> movieKeywordIds =
                            (List<Integer>) details.getOrDefault("keywordIds", Collections.emptyList());

                    Map<String, Object> credits =
                            (Map<String, Object>) details.getOrDefault("credits", Collections.emptyMap());

                    List<Integer> castIds =
                            ((List<Map<String, Object>>) credits.getOrDefault("cast", Collections.emptyList()))
                                    .stream()
                                    .map(c -> (Integer) c.get("id"))
                                    .toList();

                    List<Integer> crewIds =
                            ((List<Map<String, Object>>) credits.getOrDefault("crew", Collections.emptyList()))
                                    .stream()
                                    .map(c -> (Integer) c.get("id"))
                                    .toList();

                    // 🔥 SCORE — BONUS ZA WSPÓLNE ELEMENTY (×2 jeśli pasuje do wielu inputów)
                    for (Map<String, Object> input : inputMoviesDetails) {

                        List<Integer> inputGenres =
                                (List<Integer>) input.getOrDefault("genre_ids", Collections.emptyList());

                        List<Integer> inputKeywords =
                                (List<Integer>) input.getOrDefault("keywordIds", Collections.emptyList());

                        Map<String, Object> inputCredits =
                                (Map<String, Object>) input.getOrDefault("credits", Collections.emptyMap());

                        List<Integer> inputCast =
                                ((List<Map<String, Object>>) inputCredits.getOrDefault("cast", Collections.emptyList()))
                                        .stream()
                                        .map(c -> (Integer) c.get("id"))
                                        .toList();

                        List<Integer> inputCrew =
                                ((List<Map<String, Object>>) inputCredits.getOrDefault("crew", Collections.emptyList()))
                                        .stream()
                                        .map(c -> (Integer) c.get("id"))
                                        .toList();

                        // GENRES
                        for (Integer g : movieGenres) {
                            if (inputGenres.contains(g)) {
                                score += (isSharedAcrossAll(g, inputMoviesDetails, "genre_ids") ? 2 : 1);
                            }
                        }

                        // KEYWORDS
                        for (Integer k : movieKeywordIds) {
                            if (inputKeywords.contains(k)) {
                                score += (isSharedAcrossAll(k, inputMoviesDetails, "keywordIds") ? 2 : 1);
                            }
                        }

                        // CAST
                        for (Integer c : castIds) {
                            if (inputCast.contains(c)) {
                                score += (isSharedAcrossAllCredits(c, inputMoviesDetails, "cast") ? 2 : 1);
                            }
                        }

                        // CREW
                        for (Integer c : crewIds) {
                            if (inputCrew.contains(c)) {
                                score += (isSharedAcrossAllCredits(c, inputMoviesDetails, "crew") ? 2 : 1);
                            }
                        }
                    }

                    movie.put("score", score);
                    movie.put("genre_ids", movieGenres);
                    movie.put("keywordIds", movieKeywordIds);
                    movie.put("peopleIds", castIds.stream().limit(5).toList());
                    movie.put("release_date", movie.get("release_date"));
                    movie.put("vote_average", movie.get("vote_average"));

                    return movie;
                })
                .filter(Objects::nonNull)
                .sorted((m1, m2) ->
                        Integer.compare((Integer) m2.get("score"), (Integer) m1.get("score")))
                .toList();

        return rankedMovies;
    }
    private boolean isSharedAcrossAll(Integer id, List<Map<String, Object>> inputs, String key) {
        for (Map<String, Object> input : inputs) {
            List<Integer> list =
                    (List<Integer>) input.getOrDefault(key, Collections.emptyList());

            if (!list.contains(id)) return false;
        }
        return true;
    }

    private boolean isSharedAcrossAllCredits(Integer id, List<Map<String, Object>> inputs, String type) {
        for (Map<String, Object> input : inputs) {

            Map<String, Object> credits =
                    (Map<String, Object>) input.getOrDefault("credits", Collections.emptyMap());

            List<Map<String, Object>> list =
                    (List<Map<String, Object>>) credits.getOrDefault(type, Collections.emptyList());

            List<Integer> ids = list.stream()
                    .map(c -> (Integer) c.get("id"))
                    .toList();

            if (!ids.contains(id)) return false;
        }
        return true;
    }
}
