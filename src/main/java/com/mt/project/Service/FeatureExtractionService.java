package com.mt.project.Service;

import com.mt.project.Model.Movie;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class FeatureExtractionService {

    @Autowired
    private POSTaggerME posTagger;

    public List<String> extractFeatures(Movie movie) {

        List<String> features = new ArrayList<>();

        // overview
        if(movie.getOverview() != null) {
            features.add(movie.getOverview());
        }

        // genres
        if(movie.getGenres() != null) {
            movie.getGenres().forEach(
                    g -> features.add("genre_" + g.getName())
            );
        }

        // keywords
        if(movie.getKeywords() != null) {
            movie.getKeywords().forEach(
                    k -> features.add("keyword_" + k.getName())
            );
        }

        // people
        if(movie.getPeople() != null) {
            movie.getPeople().forEach(
                    p -> features.add("person_" + p.getName())
            );
        }

        // year
        if(movie.getRelease_date() != null) {
            features.add(
                    "year_" + movie.getRelease_date().getYear()
            );
        }

        return features;
    }

//    public List<String> extractFeaturesFromTmdb(Map<String, Object> movie) {
//
//        List<String> tokens = new ArrayList<>();
//
//        // ===== GENRES =====
//        List<Map<String, Object>> genres =
//                (List<Map<String, Object>>) movie.get("genres");
//
//        if (genres != null) {
//            for (Map<String, Object> g : genres) {
//                tokens.add(((String) g.get("name")).toLowerCase());
//            }
//        }
//
//        // ===== KEYWORDS =====
//        Map<String, Object> keywordsMap =
//                (Map<String, Object>) movie.get("keywords");
//
//        if (keywordsMap != null) {
//            List<Map<String, Object>> keywords =
//                    (List<Map<String, Object>>) keywordsMap.get("keywords");
//
//            if (keywords != null) {
//                for (Map<String, Object> k : keywords) {
//                    tokens.add(((String) k.get("name"))
//                            .toLowerCase()
//                            .replace(" ", "_"));
//                }
//            }
//        }
//
//        // ===== CAST =====
//        Map<String, Object> credits =
//                (Map<String, Object>) movie.get("credits");
//
//        if (credits != null) {
//
//            List<Map<String, Object>> cast =
//                    (List<Map<String, Object>>) credits.get("cast");
//
//            if (cast != null) {
//                for (Map<String, Object> actor : cast) {
//                    tokens.add(((String) actor.get("name"))
//                            .toLowerCase()
//                            .replace(" ", "_"));
//                }
//            }
//        }
//
//        return tokens;
//    }


    public List<String> extractFeaturesFromTmdb(Map<String, Object> movie) {

        List<String> features = new ArrayList<>();

        // 1. OVERVIEW (opis)
        String overview = (String) movie.get("overview");
        if (overview != null) {
            features.add(overview);
        }

        // 2. GENRES
        List<Map<String, Object>> genres =
                (List<Map<String, Object>>) movie.get("genres");

        if (genres != null) {
            for (Map<String, Object> g : genres) {
                features.add("genre_" + g.get("name"));
            }
        }

        // 3. RELEASE YEAR
        String releaseDate = (String) movie.get("release_date");
        if (releaseDate != null && releaseDate.length() >= 4) {
            features.add("year_" + releaseDate.substring(0, 4));
        }

        // 4. RATING
        Object rating = movie.get("vote_average");
        if (rating != null) {
            features.add("rating_" + rating);
        }

        // 5. CAST (top 5 only!)
        Map<String, Object> credits =
                (Map<String, Object>) movie.get("credits");

        if (credits != null) {
            List<Map<String, Object>> cast =
                    (List<Map<String, Object>>) credits.get("cast");

            if (cast != null) {
                cast.stream()
                        .limit(5)
                        .forEach(c -> features.add("actor_" + c.get("name")));
            }
        }

        // 6. DIRECTOR (bardzo ważne!)
        if (credits != null) {
            List<Map<String, Object>> crew =
                    (List<Map<String, Object>>) credits.get("crew");

            if (crew != null) {
                crew.stream()
                        .filter(c -> "Director".equals(c.get("job")))
                        .forEach(c -> features.add("director_" + c.get("name")));
            }
        }

        return features;
    }
}
