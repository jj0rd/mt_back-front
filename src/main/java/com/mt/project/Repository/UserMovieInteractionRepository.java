package com.mt.project.Repository;

import com.mt.project.Model.UserMovieInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserMovieInteractionRepository extends JpaRepository<UserMovieInteraction,Integer> {
    List<UserMovieInteraction> findByUserId(Integer userId);
}
