package com.mt.project.Service;

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
    public List<Map<String, Object>> recommend(Integer userId) {

        try {
            String userProfile = buildUserProfile(userId);

            if (userProfile.isBlank()) {
                return Collections.emptyList();
            }

            Set<Integer> seenMovies = interactionRepository.findByUserId(userId)
                    .stream()
                    .map(i -> i.getMovie().getTmdbId())
                    .collect(Collectors.toSet());

            DirectoryReader reader;

            try {
                reader = DirectoryReader.open(luceneDirectory);
            } catch (IndexNotFoundException e) {
                return Collections.emptyList();
            }

            IndexSearcher searcher = new IndexSearcher(reader);

            QueryParser parser = new QueryParser("content", analyzer);

            Query query = parser.parse(QueryParser.escape(userProfile));

            TopDocs results = searcher.search(query, 20);

            List<Map<String, Object>> recommendations = new ArrayList<>();

            for (ScoreDoc scoreDoc : results.scoreDocs) {

                Document doc = searcher.doc(scoreDoc.doc);
                Integer movieId = Integer.valueOf(doc.get("id"));

                if (seenMovies.contains(movieId)) continue;

                Map<String, Object> movie = tmdbService.getMovie(movieId);

                if (movie != null) {
                    recommendations.add(movie);
                }
            }

            reader.close();

            System.out.println("QUERY: " + query.toString());
            System.out.println("RESULT SIZE: " + results.scoreDocs.length);

            System.out.println("RECOMMENDATIONS SIZE: " + recommendations.size());

            return recommendations;

        } catch (Exception e) {
            throw new RuntimeException("Lucene recommendation failed", e);
        }
    }
}