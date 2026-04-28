package com.mt.project.Repository;

import com.mt.project.Model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie,Integer> {
    Optional<Movie> findByTmdbId(Integer tmdbId);
}
