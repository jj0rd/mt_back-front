package com.mt.project.Vector;

import com.mt.project.Service.FeatureExtractionService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorBuilder {
    private final FeatureExtractionService featureExtractionService;

    public VectorBuilder(FeatureExtractionService featureExtractionService) {
        this.featureExtractionService = featureExtractionService;
    }

    public FeatureVector fromMovie(Map<String, Object> movie) {

        List<String> features =
                featureExtractionService.extractFeaturesFromTmdb(movie);

        Map<String, Double> map = new HashMap<>();

        for (String f : features) {

            String key = f.toLowerCase();

            // ignorujemy numeric-like features
            if (key.startsWith("year_") || key.startsWith("rating_")) {
                continue;
            }

            map.merge(key, 1.0, Double::sum);
        }

        String date = (String) movie.get("release_date");
        if (date != null && date.length() >= 4) {

            int year = Integer.parseInt(date.substring(0, 4));
            double normalizedYear = (year - 1950) / 80.0;
            normalizedYear = Math.max(0, Math.min(1, normalizedYear));

            map.put("year", normalizedYear);
        }

        Object ratingObj = movie.get("vote_average");
        if (ratingObj != null) {

            double rating = ((Number) ratingObj).doubleValue();
            double normalizedRating = rating / 10.0;

            map.put("rating", normalizedRating);
        }

        return new FeatureVector(map);
    }

    public FeatureVector fromUser(List<Map<String, Object>> movies) {

        Map<String, Double> map = new HashMap<>();

        for (Map<String, Object> movie : movies) {

            List<String> features =
                    featureExtractionService.extractFeaturesFromTmdb(movie);

            for (String f : features) {
                map.merge(f.toLowerCase(), 1.0, Double::sum);
            }
        }

        return new FeatureVector(map);
    }
}
