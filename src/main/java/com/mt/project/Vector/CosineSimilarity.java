package com.mt.project.Vector;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CosineSimilarity {
    public double compute(FeatureVector a, FeatureVector b) {

        Map<String, Double> va = a.getValues();
        Map<String, Double> vb = b.getValues();

        double dot = 0.0;

        for (String key : va.keySet()) {
            if (vb.containsKey(key)) {
                dot += va.get(key) * vb.get(key);
            }
        }

        double normA = Math.sqrt(va.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(vb.values().stream().mapToDouble(v -> v * v).sum());

        if (normA == 0 || normB == 0) return 0.0;

        return dot / (normA * normB);
    }
}
