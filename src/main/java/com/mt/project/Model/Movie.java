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
    @ManyToMany
    @JoinTable(
            name = "movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;
    @Lob
    private String overview;
    @ManyToMany
    @JoinTable(
            name = "movie_keyword",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    private List<Keyword> keywords;
    @ManyToMany
    @JoinTable(
            name = "movie_person",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    private List<Person> people;
    private String poster_path;
    private LocalDate release_date;
    private Double voteAverage;

    @OneToMany(mappedBy = "movie")
    private List<UserMovieInteraction> interactions;

    // wektor cech
    //private Map<String, Double> vector = new HashMap<>();


    public Double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(Double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public List<UserMovieInteraction> getInteractions() {
        return interactions;
    }

    public void setInteractions(List<UserMovieInteraction> interactions) {
        this.interactions = interactions;
    }

    public Integer getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    public List<Person> getPeople() {
        return people;
    }

    public void setPeople(List<Person> people) {
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

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

}
