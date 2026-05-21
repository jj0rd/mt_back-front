package com.mt.project.Controller;

import com.mt.project.Dto.InteractionRequest;
import com.mt.project.Dto.MovieDto;
import com.mt.project.Model.Movie;
import com.mt.project.Model.User;
import com.mt.project.Model.UserMovieInteraction;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.TmdbService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interactions")
public class InteractionController {
    private final UserMovieInteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;

    public InteractionController(UserMovieInteractionRepository interactionRepository,
                                 UserRepository userRepository,MovieRepository movieRepository,
                                 TmdbService tmdbService) {
        this.interactionRepository = interactionRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.tmdbService = tmdbService;
    }
    @PostMapping("/add")
    public UserMovieInteraction addInteraction(@RequestBody InteractionRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow();

        //  1. sprawdź czy film już jest w DB
        Movie movie = movieRepository.findByTmdbId(request.getMovieId())
                .orElseGet(() -> {

                    //  2. pobierz z TMDB
                    Map<String, Object> tmdbMovie = tmdbService.getMovie(request.getMovieId());

                    if (tmdbMovie == null) {
                        throw new RuntimeException("Movie not found in TMDB");
                    }

                    //  3. mapowanie
                    Movie newMovie = tmdbService.mapToEntity(tmdbMovie);

                    //  4. zapis do DB
                    return movieRepository.save(newMovie);
                });

        //  5. zapisz interakcję
        UserMovieInteraction interaction = new UserMovieInteraction();
        interaction.setUser(user);
        interaction.setMovie(movie);
        interaction.setRating(request.getRating());

        return interactionRepository.save(interaction);
    }
    @PutMapping("/update")
    public UserMovieInteraction updateInteraction(@RequestBody InteractionRequest request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow();

        Movie movie = movieRepository.findByTmdbId(request.getMovieId())
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        UserMovieInteraction interaction = interactionRepository
                .findByUserAndMovie(user, movie)
                .orElseThrow(() -> new RuntimeException("Interaction not found"));

        interaction.setRating(request.getRating());

        return interactionRepository.save(interaction);
    }
    @GetMapping("/{userId}/ratings")
    public List<MovieDto> getUserRatings(
            @PathVariable Integer userId
    ) {

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
                    dto.setCast(movie.getPeople());
                    dto.setGenres(movie.getGenres());
                    dto.setReleaseYear(movie.getRelease_date().toString());


                    //  ocena użytkownika
                    dto.setUserRating(interaction.getRating());

                    return dto;
                })
                .toList();
    }
}
