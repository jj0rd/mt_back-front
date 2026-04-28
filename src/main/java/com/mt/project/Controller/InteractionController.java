package com.mt.project.Controller;

import com.mt.project.Dto.InteractionRequest;
import com.mt.project.Model.Movie;
import com.mt.project.Model.User;
import com.mt.project.Model.UserMovieInteraction;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.TmdbService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        // 🔥 1. sprawdź czy film już jest w DB
        Movie movie = movieRepository.findByTmdbId(request.getMovieId())
                .orElseGet(() -> {

                    // 🔥 2. pobierz z TMDB
                    Map<String, Object> tmdbMovie = tmdbService.getMovie(request.getMovieId());

                    if (tmdbMovie == null) {
                        throw new RuntimeException("Movie not found in TMDB");
                    }

                    // 🔥 3. mapowanie
                    Movie newMovie = tmdbService.mapToEntity(tmdbMovie);

                    // 🔥 4. zapis do DB
                    return movieRepository.save(newMovie);
                });

        // 🔥 5. zapisz interakcję
        UserMovieInteraction interaction = new UserMovieInteraction();
        interaction.setUser(user);
        interaction.setMovie(movie);
        interaction.setRating(request.getRating());

        return interactionRepository.save(interaction);
    }
}
