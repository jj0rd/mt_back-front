package com.mt.project.Repository;

import com.mt.project.Model.Movie;
import com.mt.project.Model.User;
import com.mt.project.Model.UserMovieInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserMovieInteractionRepository extends JpaRepository<UserMovieInteraction,Integer> {
    List<UserMovieInteraction> findByUserId(Integer userId);
    Optional<UserMovieInteraction> findByUserAndMovie(User user, Movie movie);

}
