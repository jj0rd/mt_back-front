package com.mt.project.Service;

import com.mt.project.Dto.MovieDto;
import com.mt.project.Model.Movie;
import com.mt.project.Model.ProfileSource;
import com.mt.project.Model.UserMovieInteraction;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
import com.mt.project.Service.ProfileStrategy.UserProfileProvider;
import com.mt.project.Vector.CosineSimilarity;
import com.mt.project.Vector.FeatureVector;
import com.mt.project.Vector.ScoredMovie;
import com.mt.project.Vector.VectorBuilder;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserBasedLuceneRecommendationService {

    private final UserMovieInteractionRepository interactionRepository;
    private final TmdbService tmdbService;
    private final Directory luceneDirectory;
    private final StandardAnalyzer analyzer;
    private final VectorBuilder vectorBuilder;
    private final CosineSimilarity cosineSimilarity;

    // Dynamiczna mapa strategii: Klucz = Enum, Wartość = Konkretna implementacja
    private final Map<ProfileSource, UserProfileProvider> profileProviders;

    public UserBasedLuceneRecommendationService(
            UserMovieInteractionRepository interactionRepository,
            Directory luceneDirectory,
            StandardAnalyzer analyzer,
            TmdbService tmdbService,
            VectorBuilder vectorBuilder,
            CosineSimilarity cosineSimilarity,
            List<UserProfileProvider> providerList // Spring wstrzyknie tu wszystkie komponenty implementujące interfejs
    ) {
        this.interactionRepository = interactionRepository;
        this.luceneDirectory = luceneDirectory;
        this.analyzer = analyzer;
        this.tmdbService = tmdbService;
        this.vectorBuilder = vectorBuilder;
        this.cosineSimilarity = cosineSimilarity;

        // Mapujemy listę na mapę dla szybkiego dostępu O(1)
        this.profileProviders = providerList.stream()
                .collect(Collectors.toMap(UserProfileProvider::getSourceType, p -> p));
    }

    // Helper pobierający strategię lub rzucający błąd
    private UserProfileProvider getProvider(ProfileSource source) {
        UserProfileProvider provider = profileProviders.get(source);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported profile source: " + source);
        }
        return provider;
    }

    // ==============================
    // 1. BUILD USER PROFILE (Zaktualizowany o strategię)
    // ==============================
    private String buildUserProfile(Integer userId, UserProfileProvider provider) {
        List<UserMovieInteraction> interactions = interactionRepository.findByUserId(userId);
        Map<String, Double> weights = new HashMap<>();

        for (UserMovieInteraction interaction : interactions) {
            Movie movie = interaction.getMovie();
            if (movie == null) continue;

            // 🎯 DYNAMICZNY WYBÓR ŹRÓDŁA CECH
            List<String> features = provider.getFeatures(movie);
            double weight = interaction.getRating() - 3.0;

            for (String f : features) {
                String key = f.toLowerCase();
                weights.merge(key, weight, Double::sum);
            }
        }

        return weights.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> e.getKey() + "^" + String.format("%.2f", e.getValue()))
                .collect(Collectors.joining(" "));
    }

    // ==============================
    // 2. RECOMMENDATION (Przyjmuje source!)
    // ==============================
    public List<MovieDto> recommend(Integer userId, ProfileSource source) {
        UserProfileProvider provider = getProvider(source);

        try (DirectoryReader reader = DirectoryReader.open(luceneDirectory)) {
            String userProfile = buildUserProfile(userId, provider);

            if (userProfile == null || userProfile.isBlank()) {
                return Collections.emptyList();
            }

            Set<Integer> seenMovies = interactionRepository.findByUserId(userId)
                    .stream()
                    .map(i -> i.getMovie().getTmdbId())
                    .collect(Collectors.toSet());

            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(userProfile);

            TopDocs results = searcher.search(query, 200);
            List<Integer> candidateIds = new ArrayList<>();
            int targetSize = 20;

            for (ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Integer movieId = Integer.valueOf(doc.get("id"));
                if (seenMovies.contains(movieId)) continue;
                candidateIds.add(movieId);
                if (candidateIds.size() == targetSize) break;
            }

            if (candidateIds.isEmpty()) {
                return Collections.emptyList();
            }

            Map<Integer, Map<String, Object>> tmdbBatch = tmdbService.getMoviesBatch(candidateIds);
            List<ScoredMovie> scored = new ArrayList<>();

            // 🎯 DYNAMICZNY WEKTOR UŻYTKOWNIKA
            FeatureVector userVector = buildUserVector(userId, provider);

            for (ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Integer movieId = Integer.valueOf(doc.get("id"));

                if (seenMovies.contains(movieId)) continue;

                Map<String, Object> movieRaw = tmdbBatch.get(movieId);
                if (movieRaw == null) continue;

                FeatureVector movieVector = vectorBuilder.fromMovie(movieRaw);
                double cosineScore = cosineSimilarity.compute(userVector, movieVector);

                double maxScore = Arrays.stream(results.scoreDocs)
                        .mapToDouble(sd -> sd.score)
                        .max()
                        .orElse(1.0);

                double luceneScore = scoreDoc.score;
                double normalizedLucene = Math.log1p(luceneScore) / Math.log1p(maxScore);
                double finalScore = 0.6 * normalizedLucene + 0.4 * cosineScore;

                scored.add(new ScoredMovie(movieRaw, finalScore));
            }

            return scored.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(20)
                    .map(scoredMovie -> mapToDto(scoredMovie.getMovie()))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Lucene recommendation failed", e);
        }
    }

    // ==============================
    // 3. BUILD USER VECTOR (Zaktualizowany o strategię)
    // ==============================
    private FeatureVector buildUserVector(Integer userId, UserProfileProvider provider) {
        List<UserMovieInteraction> interactions = interactionRepository.findByUserId(userId);
        Map<String, Double> map = new HashMap<>();

        double avgYear = 0;
        double avgRating = 0;
        double yearWeightSum = 0;
        double ratingWeightSum = 0;

        for (UserMovieInteraction interaction : interactions) {
            Movie movie = interaction.getMovie();
            if (movie == null) continue;

            double weight = interaction.getRating() - 3.0;

            // 🎯 DYNAMICZNY WYBÓR
            List<String> features = provider.getFeatures(movie);

            for (String f : features) {
                String key = f.toLowerCase();
                if (key.startsWith("year_") || key.startsWith("rating_")) continue;
                map.merge(key, weight, Double::sum);
            }

            // 🎯 ROK Z战略
            Integer year = provider.getReleaseYear(movie);
            if (year != null && weight > 0) {
                avgYear += year * weight;
                yearWeightSum += weight;
            }

            // 🎯 OCENA Z STRATEGII
            Double rating = provider.getVoteAverage(movie);
            if (rating != null && weight > 0) {
                avgRating += rating * weight;
                ratingWeightSum += weight;
            }
        }

        if (yearWeightSum > 0) {
            double year = avgYear / yearWeightSum;
            double normalizedYear = (year - 1950) / 80.0;
            map.put("year", Math.max(0, Math.min(1, normalizedYear)) * 2.0);
        }

        if (ratingWeightSum > 0) {
            double rating = avgRating / ratingWeightSum;
            map.put("rating", (rating / 10.0) * 1.5);
        }

        return new FeatureVector(map);
    }

    public MovieDto mapToDto(Map<String, Object> movie) {

        MovieDto dto = new MovieDto();

        dto.id = (Integer) movie.get("id");
        dto.title = (String) movie.get("title");
        dto.overview = (String) movie.get("overview");
        dto.posterPath = (String) movie.get("poster_path");

        dto.rating = movie.get("vote_average") != null
                ? ((Number) movie.get("vote_average")).doubleValue()
                : null;

        String date = (String) movie.get("release_date");
        if (date != null && date.length() >= 4) {
            dto.releaseYear = date.substring(0, 4);
        }

        // GENRES
        List<Map<String, Object>> genres =
                (List<Map<String, Object>>) movie.get("genres");

        if (genres != null) {
            dto.genres = genres.stream()
                    .map(g -> (String) g.get("name"))
                    .toList();
        }

        // CAST (top 5)
        Map<String, Object> credits =
                (Map<String, Object>) movie.get("credits");

        if (credits != null) {
            List<Map<String, Object>> cast =
                    (List<Map<String, Object>>) credits.get("cast");

            if (cast != null) {
                dto.cast = cast.stream()
                        .limit(5)
                        .map(c -> (String) c.get("name"))
                        .toList();
            }
        }

        return dto;
    }

    public MovieDto getMovieToRate(Integer userId) {

        // 🔥 filmy już obejrzane
        Set<Integer> seenMovies = interactionRepository.findByUserId(userId)
                .stream()
                .map(i -> i.getMovie().getTmdbId())
                .collect(Collectors.toSet());

        Random random = new Random();

        // 🔁 próbujemy kilka razy znaleźć nieoglądany film
        for (int attempt = 0; attempt < 20; attempt++) {

            Map<String, Object> movieMap = tmdbService.getRandomTopRatedMovie();

            if (movieMap == null) continue;

            Integer movieId = (Integer) movieMap.get("id");

            // 🚀 pomiń już oglądane
            if (movieId != null && !seenMovies.contains(movieId)) {
                return mapToMovieDto(movieMap);
            }
        }

        throw new RuntimeException("Brak filmów do oceny");
    }
    private MovieDto mapToMovieDto(Map<String, Object> movieMap) {

        MovieDto dto = new MovieDto();

        dto.setId((Integer) movieMap.get("id"));
        dto.setTitle((String) movieMap.get("title"));
        dto.setOverview((String) movieMap.get("overview"));

        // data premiery → tylko rok
        String releaseDate = (String) movieMap.get("release_date");
        if (releaseDate != null && releaseDate.length() >= 4) {
            dto.setReleaseYear(releaseDate.substring(0, 4));
        }

        dto.setRating(
                movieMap.get("vote_average") != null
                        ? ((Number) movieMap.get("vote_average")).doubleValue()
                        : null
        );

        // 🎬 poster URL (NOWE)
        String posterPath = (String) movieMap.get("poster_path");
        if (posterPath != null) {
            dto.setPosterPath("https://image.tmdb.org/t/p/w500" + posterPath);
        } else {
            dto.setPosterPath(null);
        }

        // TMDB top_rated NIE zawiera gatunków i castu → ustawiamy puste
        dto.setGenres(List.of());
        dto.setCast(List.of());

        return dto;
    }

}