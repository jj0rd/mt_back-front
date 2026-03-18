package com.mt.project.Dto;

import java.util.List;

public class MovieFuzzyResponse {
    private Integer id;
    private String title;
    private String releaseDate;
    private Double voteAverage;
    private List<Integer> genreIds;
    private List<Integer> keywordIds;
    private List<Integer> peopleIds;
    private Integer score;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(Double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public List<Integer> getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(List<Integer> genreIds) {
        this.genreIds = genreIds;
    }

    public List<Integer> getKeywordIds() {
        return keywordIds;
    }

    public void setKeywordIds(List<Integer> keywordIds) {
        this.keywordIds = keywordIds;
    }

    public List<Integer> getPeopleIds() {
        return peopleIds;
    }

    public void setPeopleIds(List<Integer> peopleIds) {
        this.peopleIds = peopleIds;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
