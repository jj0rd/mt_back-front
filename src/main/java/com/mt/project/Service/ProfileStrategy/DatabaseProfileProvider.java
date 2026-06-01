package com.mt.project.Service.ProfileStrategy;

import com.mt.project.Model.Movie;
import com.mt.project.Model.ProfileSource;
import com.mt.project.Service.FeatureExtractionService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseProfileProvider implements UserProfileProvider {

    private final FeatureExtractionService featureExtractionService;

    public DatabaseProfileProvider(FeatureExtractionService featureExtractionService) {
        this.featureExtractionService = featureExtractionService;
    }

    @Override
    public ProfileSource getSourceType() {
        return ProfileSource.DATABASE;
    }

    @Override
    public List<String> getFeatures(Movie dbMovie) {
        return featureExtractionService.extractFeatures(dbMovie);
    }

    @Override
    public Integer getReleaseYear(Movie dbMovie) {
        return dbMovie.getRelease_date() != null ? dbMovie.getRelease_date().getYear() : null;
    }

    @Override
    public Double getVoteAverage(Movie dbMovie) {
        return dbMovie.getVoteAverage();
    }
}
