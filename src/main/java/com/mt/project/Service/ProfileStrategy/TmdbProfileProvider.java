package com.mt.project.Service.ProfileStrategy;

import com.mt.project.Model.Movie;
import com.mt.project.Model.ProfileSource;
import com.mt.project.Service.FeatureExtractionService;
import com.mt.project.Service.TmdbService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class TmdbProfileProvider implements UserProfileProvider {

    private final TmdbService tmdbService;
    private final FeatureExtractionService featureExtractionService;

    public TmdbProfileProvider(TmdbService tmdbService, FeatureExtractionService featureExtractionService) {
        this.tmdbService = tmdbService;
        this.featureExtractionService = featureExtractionService;
    }

    @Override
    public ProfileSource getSourceType() {
        return ProfileSource.TMDB;
    }

    private Map<String, Object> getTmdbMovie(Movie dbMovie) {
        return tmdbService.getMovie(dbMovie.getTmdbId());
    }

    @Override
    public List<String> getFeatures(Movie dbMovie) {
        Map<String, Object> movieRaw = getTmdbMovie(dbMovie);
        if (movieRaw == null) return Collections.emptyList();
        return featureExtractionService.extractFeaturesFromTmdb(movieRaw);
    }

    @Override
    public Integer getReleaseYear(Movie dbMovie) {
        Map<String, Object> movieRaw = getTmdbMovie(dbMovie);
        if (movieRaw == null) return null;
        String releaseDate = (String) movieRaw.get("release_date");
        if (releaseDate != null && releaseDate.length() >= 4) {
            return Integer.parseInt(releaseDate.substring(0, 4));
        }
        return null;
    }

    @Override
    public Double getVoteAverage(Movie dbMovie) {
        Map<String, Object> movieRaw = getTmdbMovie(dbMovie);
        if (movieRaw == null) return null;
        Object ratingObj = movieRaw.get("vote_average");
        return ratingObj != null ? ((Number) ratingObj).doubleValue() : null;
    }
}
