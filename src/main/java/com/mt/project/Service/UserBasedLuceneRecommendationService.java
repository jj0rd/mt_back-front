package com.mt.project.Service;

import com.mt.project.Dto.MovieDto;
import com.mt.project.Model.Movie;
import com.mt.project.Model.UserMovieInteraction;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
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

    public UserBasedLuceneRecommendationService(
            FeatureExtractionService featureExtractionService,
            UserMovieInteractionRepository interactionRepository,
            MovieRepository movieRepository,
            Directory luceneDirectory,
            StandardAnalyzer analyzer,
            TmdbService tmdbService
    ) {
        this.featureExtractionService = featureExtractionService;
        this.interactionRepository = interactionRepository;
        this.movieRepository = movieRepository;
        this.luceneDirectory = luceneDirectory;
        this.analyzer = analyzer;
        this.tmdbService = tmdbService;
    }

    // ==============================
    // 1. BUILD USER PROFILE
    // ==============================
    private String buildUserProfile(Integer userId) {

        List<UserMovieInteraction> interactions =
                interactionRepository.findByUserId(userId);

        StringBuilder profile = new StringBuilder();

        for (UserMovieInteraction interaction : interactions) {

            Integer movieId = interaction.getMovie().getTmdbId();

            Map<String, Object> movie =
                    tmdbService.getMovie(movieId);

            if (movie == null) continue;

            List<String> features =
                    featureExtractionService.extractFeaturesFromTmdb(movie);

            int weight = Math.max(1, interaction.getRating());

            profile.append(
                    String.join(" ", features)
            ).append(" ".repeat(weight));
        }

        return profile.toString().trim().toLowerCase();
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
            Query query = parser.parse(QueryParser.escape(userProfile));

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

            for (Integer id : candidateIds) {

                Map<String, Object> movieRaw = tmdbBatch.get(id);

                if (movieRaw == null) continue;

                MovieDto dto = mapToDto(movieRaw);

                recommendations.add(dto);
            }

            return recommendations;

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
}