package com.mt.project.Service;

import com.mt.project.Dto.MovieDto;
import com.mt.project.Model.Movie;
import com.mt.project.Model.UserMovieInteraction;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
import com.mt.project.Vector.CosineSimilarity;
import com.mt.project.Vector.FeatureVector;
import com.mt.project.Vector.ScoredMovie;
import com.mt.project.Vector.VectorBuilder;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserBasedLuceneRecommendationService {

    private final FeatureExtractionService featureExtractionService;
    private final UserMovieInteractionRepository interactionRepository;
    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    private final Directory luceneDirectory;
    private final StandardAnalyzer analyzer;
    private final VectorBuilder vectorBuilder;
    private final CosineSimilarity cosineSimilarity;

    public UserBasedLuceneRecommendationService(
            FeatureExtractionService featureExtractionService,
            UserMovieInteractionRepository interactionRepository,
            MovieRepository movieRepository,
            Directory luceneDirectory,
            StandardAnalyzer analyzer,
            TmdbService tmdbService,
            VectorBuilder vectorBuilder,
            CosineSimilarity cosineSimilarity
    ) {
        this.featureExtractionService = featureExtractionService;
        this.interactionRepository = interactionRepository;
        this.movieRepository = movieRepository;
        this.luceneDirectory = luceneDirectory;
        this.analyzer = analyzer;
        this.tmdbService = tmdbService;
        this.vectorBuilder = vectorBuilder;
        this.cosineSimilarity = cosineSimilarity;
    }

    // ==============================
    // 1. BUILD USER PROFILE
    // ==============================
    private String buildUserProfile(Integer userId) {

        List<UserMovieInteraction> interactions =
                interactionRepository.findByUserId(userId);

        Map<String, Integer> weights = new HashMap<>();

        for (UserMovieInteraction interaction : interactions) {

            Integer movieId = interaction.getMovie().getTmdbId();

            Map<String, Object> movie =
                    tmdbService.getMovie(movieId);

            if (movie == null) continue;

            List<String> features =
                    featureExtractionService.extractFeaturesFromTmdb(movie);

            int weight = Math.max(1, interaction.getRating());

            for (String f : features) {
                weights.merge(f.toLowerCase(), weight, Integer::sum);
            }
        }

        // 🔥 budowa query z boostem
        return weights.entrySet().stream()
                .map(e -> e.getKey() + "^" + e.getValue())
                .collect(Collectors.joining(" "));
    }

    // ==============================
    // 2. RECOMMENDATION
    // ==============================
    public List<MovieDto> recommend(Integer userId) {

        try (DirectoryReader reader = DirectoryReader.open(luceneDirectory)) {

            String userProfile = buildUserProfile(userId);

            if (userProfile == null || userProfile.isBlank()) {
                return Collections.emptyList();
            }

            // 🔥 filmy już obejrzane
            Set<Integer> seenMovies = interactionRepository.findByUserId(userId)
                    .stream()
                    .map(i -> i.getMovie().getTmdbId())
                    .collect(Collectors.toSet());

            IndexSearcher searcher = new IndexSearcher(reader);

            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(userProfile);

            TopDocs results = searcher.search(query, 20);

            List<Integer> candidateIds = new ArrayList<>();

            for (ScoreDoc scoreDoc : results.scoreDocs) {

                Document doc = searcher.doc(scoreDoc.doc);
                Integer movieId = Integer.valueOf(doc.get("id"));

                if (!seenMovies.contains(movieId)) {
                    candidateIds.add(movieId);
                }
            }

            if (candidateIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 🔥 BATCH CALL do TMDB (ważne!)
            Map<Integer, Map<String, Object>> tmdbBatch =
                    tmdbService.getMoviesBatch(candidateIds);

            List<MovieDto> recommendations = new ArrayList<>();

            FeatureVector userVector = buildUserVector(userId);

            List<ScoredMovie> scored = new ArrayList<>();

            for (ScoreDoc scoreDoc : results.scoreDocs) {

                Document doc = searcher.doc(scoreDoc.doc);
                Integer movieId = Integer.valueOf(doc.get("id"));

                if (seenMovies.contains(movieId)) continue;

                Map<String, Object> movieRaw = tmdbBatch.get(movieId);
                if (movieRaw == null) continue;

                // 🔹 VECTOR filmu
                FeatureVector movieVector =
                        vectorBuilder.fromMovie(movieRaw);

                // 🔹 COSINE
                double cosineScore =
                        cosineSimilarity.compute(userVector, movieVector);

                // 🔹 LUCENE
                double luceneScore = scoreDoc.score;
                double normalizedLucene = normalizeLucene(luceneScore);

                // 🔥 FINAL SCORE
                double finalScore = 0.7 * normalizedLucene + 0.3 * cosineScore;

                scored.add(new ScoredMovie(movieRaw, finalScore));
            }

            return scored.stream()
                    .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                    .limit(20)
                    .map(s -> mapToDto(s.getMovie()))
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Lucene recommendation failed", e);
        }
    }

    public MovieDto mapToDto(Map<String, Object> movie) {

        MovieDto dto = new MovieDto();

        dto.id = (Integer) movie.get("id");
        dto.title = (String) movie.get("title");
        dto.overview = (String) movie.get("overview");
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
    private FeatureVector buildUserVector(Integer userId) {

        List<UserMovieInteraction> interactions =
                interactionRepository.findByUserId(userId);

        Map<String, Double> map = new HashMap<>();

        for (UserMovieInteraction interaction : interactions) {

            Integer movieId = interaction.getMovie().getTmdbId();

            Map<String, Object> movie = tmdbService.getMovie(movieId);
            if (movie == null) continue;

            List<String> features =
                    featureExtractionService.extractFeaturesFromTmdb(movie);

            double weight = Math.max(1, interaction.getRating());

            for (String f : features) {
                map.merge(f.toLowerCase(), weight, Double::sum);
            }
        }

        return new FeatureVector(map);
    }

    private double normalizeLucene(double score) {
        return score / (score + 1);
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