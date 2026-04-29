//package com.mt.project.Vector;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class MovieVectorBuilder {
//    public Map<String, Double> buildMovieVector(Map<String, Object> movie) {
//
//        Map<String, Double> vector = new HashMap<>();
//
//        List<String> features =
//                (List<String>) movie.get("features");
//        // albo z featureExtractionService
//
//        if (features == null) return vector;
//
//        for (String feature : features) {
//
//            String key = feature.toLowerCase();
//
//            vector.merge(key, 1.0, Double::sum);
//        }
//
//        return vector;
//    }
//}
