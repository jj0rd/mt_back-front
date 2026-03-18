//package com.mt.project.controllerTest;
//
//import org.apache.commons.text.similarity.LevenshteinDistance;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.client.RestClientException;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/movies")  // dobry praktyka - dodaj prefix
//public class TestController {
//
//    @Value("${omdb.api.key}")
//    private String apiKey;
//
//    @Value("${omdb.api.url}")
//    private String apiUrl;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @GetMapping("/{title}")
//    public ResponseEntity<?> getMovie(@PathVariable String title) {
//        try {
//            // Walidacja wejścia
//            if (title == null || title.trim().isEmpty()) {
//                return ResponseEntity.badRequest()
//                        .body(Collections.singletonMap("Error", "Title cannot be empty"));
//            }
//
//            String normalizedTitle = title.trim();
//
//            // 1️⃣ Dokładny tytuł
//            Map<String, Object> exactResult = fetchMovieByExactTitle(normalizedTitle);
//            if (exactResult != null && "True".equals(exactResult.get("Response"))) {
//                return ResponseEntity.ok(enrichMovieData(exactResult));
//            }
//
//            // 2️⃣ Wyszukiwanie podobnych tytułów
//            List<Map<String, Object>> similarMovies = searchSimilarMovies(normalizedTitle);
//
//            if (similarMovies.isEmpty()) {
//                return ResponseEntity.status(404)
//                        .body(Collections.singletonMap("Error", "Movie not found!"));
//            }
//
//            // 3️⃣ Sortowanie według podobieństwa Levenshtein
//            List<Map<String, Object>> sortedMovies = rankMoviesBySimilarity(similarMovies, normalizedTitle);
//
//            // 4️⃣ Zwróć top 3 wyniki
//            Map<String, Object> response = new HashMap<>();
//            response.put("query", normalizedTitle);
//            response.put("exactMatch", false);
//            response.put("suggestions", sortedMovies.stream().limit(3).collect(Collectors.toList()));
//            response.put("totalResults", similarMovies.size());
//
//            return ResponseEntity.ok(response);
//
//        } catch (RestClientException e) {
//            // Błąd połączenia z OMDb API
//            return ResponseEntity.status(503)
//                    .body(Collections.singletonMap("Error", "Movie service unavailable"));
//        } catch (Exception e) {
//            // Inne nieoczekiwane błędy
//            return ResponseEntity.status(500)
//                    .body(Collections.singletonMap("Error", "Internal server error"));
//        }
//    }
//
//    private Map<String, Object> fetchMovieByExactTitle(String title) {
//        String url = apiUrl + "?apikey=" + apiKey + "&t=" + title;
//        return restTemplate.getForObject(url, Map.class);
//    }
//
//    private List<Map<String, Object>> searchSimilarMovies(String title) {
//        String url = apiUrl + "?apikey=" + apiKey + "&s=" + title;
//        Map<String, Object> searchResult = restTemplate.getForObject(url, Map.class);
//
//        if (searchResult != null && "True".equals(searchResult.get("Response"))) {
//            return (List<Map<String, Object>>) searchResult.get("Search");
//        }
//        return Collections.emptyList();
//    }
//
//    private List<Map<String, Object>> rankMoviesBySimilarity(
//            List<Map<String, Object>> movies, String query) {
//
//        LevenshteinDistance ld = new LevenshteinDistance();
//        String lowerQuery = query.toLowerCase();
//
//        return movies.stream()
//                .map(movie -> {
//                    // Oblicz podobieństwo dla każdego filmu
//                    String title = (String) movie.get("Title");
//                    int distance = ld.apply(lowerQuery, title.toLowerCase());
//
//                    // Dodaj score do mapy (możesz to wykorzystać później)
//                    Map<String, Object> enrichedMovie = new HashMap<>(movie);
//                    enrichedMovie.put("similarityScore", 100 - distance); // im wyższy, tym lepszy
//                    enrichedMovie.put("levenshteinDistance", distance);
//
//                    return enrichedMovie;
//                })
//                .sorted(Comparator.comparingInt(
//                        m -> (int) ((Map) m).get("levenshteinDistance")
//                ))
//                .collect(Collectors.toList());
//    }
//
//    private Map<String, Object> enrichMovieData(Map<String, Object> movie) {
//        // Możesz dodać dodatkowe informacje o filmie
//        movie.put("searchedAt", new Date());
//        return movie;
//    }
//
//    // Dodatkowy endpoint do wyszukiwania z parametrami
//    @GetMapping("/search")
//    public ResponseEntity<?> searchMovies(
//            @RequestParam String query,
//            @RequestParam(required = false, defaultValue = "3") int limit) {
//
//        return getMovie(query); // reuse istniejącej logiki
//    }
//
//    @Value("${tmdb.api.key}")
//    private String tmdbapiKey;
//
//    @Value("${tmdb.api.url}")
//    private String tmdbapiUrl;
//
//
//    @GetMapping("/search/{title}")
//    public Object searchMovie(@PathVariable String title) {
//
//        // Tworzymy URL TMDb do wyszukiwania filmów
//        String url = tmdbapiUrl + "/search/movie?api_key=" + tmdbapiKey + "&query=" + title;
//
//        // Pobieramy dane jako Map (JSON automatycznie mapowany)
//        Map<String, Object> result = restTemplate.getForObject(url, Map.class);
//
//        if (result == null || !"True".equals(result.getOrDefault("Response", "True"))) {
//            return Map.of("Error", "Movie not found!");
//        }
//
//        // Zwracamy listę filmów z pola "results"
//        List<Map<String, Object>> movies = (List<Map<String, Object>>) result.get("results");
//        return movies;
//    }
//}