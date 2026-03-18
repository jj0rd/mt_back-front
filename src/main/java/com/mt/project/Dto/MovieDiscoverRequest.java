package com.mt.project.Dto;

import java.util.List;

public class MovieDiscoverRequest {
    private List<Integer> genre;

    private Integer yearFrom;
    private Integer yearTo;
    private Double rating;
    private String language;
    private List<Integer> people;
    private List<Integer> keywords;

    public List<Integer> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Integer> keywords) {
        this.keywords = keywords;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public void setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public void setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
    }

    public List<Integer> getPeople() {
        return people;
    }

    public void setPeople(List<Integer> people) {
        this.people = people;
    }

    public List<Integer> getGenre() {
        return genre;
    }

    public void setGenre(List<Integer> genre) {
        this.genre = genre;
    }


    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
