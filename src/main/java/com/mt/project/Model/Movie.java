package com.mt.project.Model;

import jakarta.persistence.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class Movie {
    @Id
    @GeneratedValue
    private Integer id;
    private String title;
    private Integer tmdbId;
    @ElementCollection
    private List<String> genres;
    @Lob
    private String overview;
    @ElementCollection
    private List<String> keywords;
    @ElementCollection
    private List<String> people;
    private String poster_path;
    private LocalDate release_date;

    @OneToMany(mappedBy = "movie")
    private List<UserMovieInteraction> interactions;

    // wektor cech
    //private Map<String, Double> vector = new HashMap<>();


    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getPeople() {
        return people;
    }

    public void setPeople(List<String> people) {
        this.people = people;
    }

    public String getPoster_path() {
        return poster_path;
    }

    public void setPoster_path(String poster_path) {
        this.poster_path = poster_path;
    }

    public LocalDate getRelease_date() {
        return release_date;
    }

    public void setRelease_date(LocalDate release_date) {
        this.release_date = release_date;
    }

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

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

}
