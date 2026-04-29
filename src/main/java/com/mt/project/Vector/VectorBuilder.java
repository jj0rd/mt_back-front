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
            map.merge(f.toLowerCase(), 1.0, Double::sum);
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
