package com.mt.project.Service.ProfileStrategy;

import com.mt.project.Model.Movie;
import com.mt.project.Model.ProfileSource;

import java.util.List;

public interface UserProfileProvider {
    ProfileSource getSourceType();
    List<String> getFeatures(Movie dbMovie);
    Integer getReleaseYear(Movie dbMovie);
    Double getVoteAverage(Movie dbMovie);
}
