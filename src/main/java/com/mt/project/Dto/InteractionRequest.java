package com.mt.project.Dto;

public class InteractionRequest {
    private Integer userId;
    private Integer movieId;
    private int rating;

    public Integer getUserId() {
        return userId;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public int getRating() {
        return rating;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
