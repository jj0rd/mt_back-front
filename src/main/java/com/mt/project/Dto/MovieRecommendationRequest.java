package com.mt.project.Dto;

import java.util.List;

public class MovieRecommendationRequest {
    private List<String> movieTitles;

    public List<String> getMovieTitles() {
        return movieTitles;
    }

    public void setMovieTitles(List<String> movieTitles) {
        this.movieTitles = movieTitles;
    }
}
