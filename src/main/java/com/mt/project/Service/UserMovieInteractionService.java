package com.mt.project.Service;

import com.mt.project.Dto.InteractionRequest;
import com.mt.project.Dto.InteractionResponse;
import com.mt.project.Dto.MovieDto;
import com.mt.project.Model.Movie;
import com.mt.project.Model.User;
import com.mt.project.Model.UserMovieInteraction;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.TmdbService;
import org.springframework.web.bind.annotation.*;
import com.mt.project.Model.Genre;
import com.mt.project.Model.Person;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserMovieInteractionService {

    private final UserMovieInteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    public UserMovieInteractionService(
            UserMovieInteractionRepository interactionRepository,
            UserRepository userRepository,
            MovieRepository movieRepository,
            TmdbService tmdbService) {

        this.interactionRepository = interactionRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.tmdbService = tmdbService;
    }

    public InteractionResponse addInteraction(
            InteractionRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow();

        Movie movie = movieRepository.findByTmdbId(request.getMovieId())
                .orElseGet(() -> {

                    Map<String, Object> tmdbMovie =
                            tmdbService.getMovie(request.getMovieId());

                    if (tmdbMovie == null) {
                        throw new RuntimeException("Movie not found in TMDB");
                    }

                    Movie newMovie =
                            tmdbService.mapToEntity(tmdbMovie);

                    return movieRepository.save(newMovie);
                });

        UserMovieInteraction interaction =
                new UserMovieInteraction();

        interaction.setUser(user);
        interaction.setMovie(movie);
        interaction.setRating(request.getRating());

        UserMovieInteraction savedInteraction =
                interactionRepository.save(interaction);

        InteractionResponse response =
                new InteractionResponse();

        response.setMovieId(movie.getTmdbId());
        response.setMovieTitle(movie.getTitle());
        response.setRating(savedInteraction.getRating());

        return response;
    }

    public InteractionResponse updateInteraction(
            InteractionRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow();

        Movie movie = movieRepository.findByTmdbId(request.getMovieId())
                .orElseThrow(() ->
                        new RuntimeException("Movie not found"));

        UserMovieInteraction interaction =
                interactionRepository.findByUserAndMovie(user, movie)
                        .orElseThrow(() ->
                                new RuntimeException("Interaction not found"));

        interaction.setRating(request.getRating());

        UserMovieInteraction savedInteraction =
                interactionRepository.save(interaction);

        InteractionResponse response =
                new InteractionResponse();

        response.setMovieId(movie.getTmdbId());
        response.setMovieTitle(movie.getTitle());
        response.setRating(savedInteraction.getRating());

        return response;
    }

    public List<MovieDto> getUserRatings(Integer userId) {

        List<UserMovieInteraction> interactions =
                interactionRepository.findByUserId(userId);

        return interactions.stream()
                .map(interaction -> {

                    Movie movie = interaction.getMovie();

                    MovieDto dto = new MovieDto();

                    dto.setId(movie.getTmdbId());
                    dto.setTitle(movie.getTitle());
                    dto.setOverview(movie.getOverview());
                    dto.setPosterPath(movie.getPoster_path());

                    dto.setCast(
                            movie.getPeople().stream()
                                    .map(Person::getName)
                                    .toList()
                    );

                    dto.setGenres(
                            movie.getGenres().stream()
                                    .map(Genre::getName)
                                    .toList()
                    );

                    dto.setReleaseYear(
                            movie.getRelease_date().toString()
                    );

                    dto.setUserRating(interaction.getRating());

                    return dto;
                })
                .toList();
    }
}
