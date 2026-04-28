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
        List<String> tokens = new ArrayList<>();

        // genres
        for (String genre : movie.getGenres()) {
            tokens.add(genre.toLowerCase());
        }

        // ===== KEYWORDS  =====
//        if (movie.getKeywords() != null) {
//            for (String kw : movie.getKeywords()) {
//                    tokens.add(kw.toLowerCase().replace(" ", "_"));
//            }
//        }

        // peope cast+director
        if (movie.getPeople() != null) {
            for (String actor : movie.getPeople()) {
                tokens.add(actor.toLowerCase().replace(" ", "_"));
            }
        }

        // ===== OVERVIEW + NLP =====
        if (movie.getOverview() != null) {

            SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

            String[] words = tokenizer.tokenize(
                    movie.getOverview().toLowerCase()
            );

            String[] tags = posTagger.tag(words);

            for (int i = 0; i < words.length; i++) {

                String word = words[i];
                String tag = tags[i];

                // tylko rzeczowniki i przymiotniki
                if (tag.startsWith("NN") || tag.startsWith("JJ")) {

                    String clean = word.replaceAll("[^a-z]", "");

                    if (clean.length() > 2) {
                        tokens.add(clean);
                    }
                }
            }
        }

        return tokens;
    }

    public List<String> extractFeaturesFromTmdb(Map<String, Object> movie) {

        List<String> tokens = new ArrayList<>();

        // ===== GENRES =====
        List<Map<String, Object>> genres =
                (List<Map<String, Object>>) movie.get("genres");

        if (genres != null) {
            for (Map<String, Object> g : genres) {
                tokens.add(((String) g.get("name")).toLowerCase());
            }
        }

        // ===== KEYWORDS =====
        Map<String, Object> keywordsMap =
                (Map<String, Object>) movie.get("keywords");

        if (keywordsMap != null) {
            List<Map<String, Object>> keywords =
                    (List<Map<String, Object>>) keywordsMap.get("keywords");

            if (keywords != null) {
                for (Map<String, Object> k : keywords) {
                    tokens.add(((String) k.get("name"))
                            .toLowerCase()
                            .replace(" ", "_"));
                }
            }
        }

        // ===== CAST =====
        Map<String, Object> credits =
                (Map<String, Object>) movie.get("credits");

        if (credits != null) {

            List<Map<String, Object>> cast =
                    (List<Map<String, Object>>) credits.get("cast");

            if (cast != null) {
                for (Map<String, Object> actor : cast) {
                    tokens.add(((String) actor.get("name"))
                            .toLowerCase()
                            .replace(" ", "_"));
                }
            }
        }

        return tokens;
    }
}
