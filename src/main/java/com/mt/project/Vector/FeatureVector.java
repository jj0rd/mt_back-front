package com.mt.project.Vector;

import java.util.Map;

public class FeatureVector {
    private final Map<String, Double> values;

    public FeatureVector(Map<String, Double> values) {
        this.values = values;
    }

    public Map<String, Double> getValues() {
        return values;
    }
}
