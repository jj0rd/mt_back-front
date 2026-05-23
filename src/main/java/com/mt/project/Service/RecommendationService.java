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


    // =========================
    // WAGI
    // =========================

    private static final int GENRE_WEIGHT = 8;
    private static final int KEYWORD_WEIGHT = 2;
    private static final int CAST_WEIGHT = 7;
    private static final int DIRECTOR_WEIGHT = 10;

    private static final int DIRECTOR_SOURCE_BONUS = 15;
    private static final int CAST_SOURCE_BONUS = 10;
    private static final int GENRE_SOURCE_BONUS = 8;
    private static final int KEYWORD_SOURCE_BONUS = 2;
    private static final int SIMILAR_SOURCE_BONUS = 20;

    private final RestTemplate restTemplate;
    private final FeatureExtractionService featureExtractionService;

    public RecommendationService(RestTemplate restTemplate, FeatureExtractionService featureExtractionService) {
        this.restTemplate = restTemplate;
        this.featureExtractionService = featureExtractionService;
    }



    // 🔹 Zamiana tytułu filmu na TMDb ID
    public Integer findMovieIdByTitle(String title) {
        try {
            String encoded = URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
            System.out.println("encoded = " + encoded);
            String url = tmdbApiUrl + "/search/movie?api_key=" + tmdbApiKey + "&query=" + encoded;
            System.out.println("url = " + url);

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

    public List<Map<String, Object>> recommendMovies(List<String> movieTitles) {

        // =========================
        // INPUT MOVIES
        // =========================
        List<Integer> movieIds = movieTitles.stream()
                .map(this::findMovieIdByTitle)
                .filter(Objects::nonNull)
                .toList();

        if (movieIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> inputMovies = movieIds.stream()
                .map(this::getMovieDetails)
                .filter(Objects::nonNull)
                .toList();

        // =========================
        // FEATURES (LIST OF STRING FEATURES)
        // =========================
        List<List<String>> inputFeatures = inputMovies.stream()
                .map(featureExtractionService::extractFeaturesFromTmdb)
                .toList();

        // =========================
        // CANDIDATES (MULTI SOURCE)
        // =========================
        Map<Integer, Map<String, Object>> candidates =
                buildCandidates(inputMovies, movieIds);

        movieIds.forEach(candidates::remove);

        // =========================
        // RANKING
        // =========================
        return candidates.values().stream()
                .map(m -> {
                    int score = calculateScore(m, inputFeatures);
                    m.put("score", score);
                    return m;
                })
                .sorted((a, b) ->
                        Integer.compare((Integer) b.get("score"), (Integer) a.get("score")))
                .limit(50)
                .toList();
    }


    private Map<Integer, Map<String, Object>> buildCandidates(
            List<Map<String, Object>> inputMovies,
            List<Integer> inputIds
    ) {

        Map<Integer, Map<String, Object>> candidates = new HashMap<>();

        // =========================
        // GENRES
        // =========================
        Set<Integer> genres = new HashSet<>();

        for (Map<String, Object> m : inputMovies) {
            genres.addAll((List<Integer>) m.getOrDefault("genre_ids", List.of()));
        }

        if (!genres.isEmpty()) {

            String url = tmdbApiUrl + "/discover/movie"
                    + "?api_key=" + tmdbApiKey
                    + "&with_genres=" + join(genres)
                    + "&vote_count.gte=200";

            merge(candidates, fetch(url), "genre");
        }

        // =========================
        // DIRECTORS
        // =========================
        Set<Integer> directors = new HashSet<>();

        for (Map<String, Object> m : inputMovies) {

            Map<String, Object> credits =
                    (Map<String, Object>) m.getOrDefault("credits", Map.of());

            List<Map<String, Object>> crew =
                    (List<Map<String, Object>>) credits.getOrDefault("crew", List.of());

            crew.stream()
                    .filter(c -> "Director".equals(c.get("job")))
                    .map(c -> (Integer) c.get("id"))
                    .forEach(directors::add);
        }

        if (!directors.isEmpty()) {

            String url = tmdbApiUrl + "/discover/movie"
                    + "?api_key=" + tmdbApiKey
                    + "&with_crew=" + join(directors)
                    + "&vote_count.gte=100";

            merge(candidates, fetch(url), "director");
        }

        // =========================
        // CAST
        // =========================
        Set<Integer> cast = new HashSet<>();

        for (Map<String, Object> m : inputMovies) {

            Map<String, Object> credits =
                    (Map<String, Object>) m.getOrDefault("credits", Map.of());

            List<Map<String, Object>> castList =
                    (List<Map<String, Object>>) credits.getOrDefault("cast", List.of());

            castList.stream()
                    .limit(5)
                    .map(c -> (Integer) c.get("id"))
                    .forEach(cast::add);
        }

        if (!cast.isEmpty()) {

            String url = tmdbApiUrl + "/discover/movie"
                    + "?api_key=" + tmdbApiKey
                    + "&with_cast=" + join(cast)
                    + "&vote_count.gte=100";

            merge(candidates, fetch(url), "cast");
        }

        // =========================
        // KEYWORDS (LOW PRIORITY)
        // =========================
        Set<Integer> keywords = new HashSet<>();

        for (Map<String, Object> m : inputMovies) {
            keywords.addAll((List<Integer>) m.getOrDefault("keywordIds", List.of()));
        }

        if (!keywords.isEmpty()) {

            String url = tmdbApiUrl + "/discover/movie"
                    + "?api_key=" + tmdbApiKey
                    + "&with_keywords=" + join(keywords);

            merge(candidates, fetch(url), "keyword");
        }

        // =========================
        // SIMILAR MOVIES (VERY IMPORTANT)
        // =========================
        for (Integer id : inputIds) {

            String url = tmdbApiUrl + "/movie/" + id
                    + "/similar?api_key=" + tmdbApiKey;

            merge(candidates, fetch(url), "similar");
        }

        return candidates;
    }

    private void merge(
            Map<Integer, Map<String, Object>> candidates,
            List<Map<String, Object>> movies,
            String source
    ) {

        for (Map<String, Object> m : movies) {

            Integer id = (Integer) m.get("id");
            if (id == null) continue;

            if (!candidates.containsKey(id)) {

                m.put("sources", new HashSet<String>());
                ((Set<String>) m.get("sources")).add(source);

                candidates.put(id, m);

            } else {

                ((Set<String>) candidates.get(id).get("sources"))
                        .add(source);
            }
        }
    }

    private int calculateScore(
            Map<String, Object> movie,
            List<List<String>> inputFeatures
    ) {

        int score = 0;

        Set<String> sources =
                (Set<String>) movie.getOrDefault("sources", Set.of());

        if (sources.contains("similar")) score += 20;
        if (sources.contains("director")) score += 15;
        if (sources.contains("cast")) score += 10;
        if (sources.contains("genre")) score += 8;
        if (sources.contains("keyword")) score += 2;

        List<String> movieFeatures =
                featureExtractionService.extractFeaturesFromTmdb(movie);

        for (List<String> input : inputFeatures) {

            for (String f : movieFeatures) {

                if (input.contains(f)) {

                    if (f.startsWith("genre_")) score += 10;
                    else if (f.startsWith("director_")) score += 18;
                    else if (f.startsWith("actor_")) score += 7;
                    else if (f.startsWith("year_")) score += 6;
                    else if (f.startsWith("rating_")) score += 3;
                    else score += 1;
                }
            }
        }

        double vote =
                ((Number) movie.getOrDefault("vote_average", 0)).doubleValue();

        score += (int) (vote * 1.5);

        if (vote < 6.5) score -= 10;

        return score;
    }

    private String join(Collection<Integer> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("|"));
    }

    private List<Map<String, Object>> fetch(String url) {

        Map<String, Object> res =
                restTemplate.getForObject(url, Map.class);

        if (res == null || !res.containsKey("results")) {
            return Collections.emptyList();
        }

        return (List<Map<String, Object>>) res.get("results");
    }

    private int extractYear(String date) {
        if (date == null || date.length() < 4) return 0;
        return Integer.parseInt(date.substring(0, 4));
    }

    private int yearScore(int a, int b) {
        if (a == 0 || b == 0) return 0;

        int diff = Math.abs(a - b);

        if (diff <= 5) return 10;
        if (diff <= 10) return 7;
        if (diff <= 20) return 4;
        if (diff <= 30) return 2;

        return 0;
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
