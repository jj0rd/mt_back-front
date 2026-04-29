package com.mt.project.Vector;

import java.util.Map;

public class ScoredMovie {
    private Map<String, Object> movie;
    private double score;

    public ScoredMovie(Map<String, Object> movie, double score) {
        this.movie = movie;
        this.score = score;
    }

    public Map<String, Object> getMovie() {
        return movie;
    }

    public void setMovie(Map<String, Object> movie) {
        this.movie = movie;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
