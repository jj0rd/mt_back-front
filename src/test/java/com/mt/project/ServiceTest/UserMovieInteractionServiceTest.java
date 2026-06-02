package com.mt.project.ServiceTest;

import com.mt.project.Dto.InteractionRequest;
import com.mt.project.Dto.InteractionResponse;
import com.mt.project.Dto.MovieDto;
import com.mt.project.Model.*;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.TmdbService;
import com.mt.project.Service.UserMovieInteractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserMovieInteractionServiceTest {

    @Mock
    private UserMovieInteractionRepository interactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TmdbService tmdbService;

    @InjectMocks
    private UserMovieInteractionService service;

    // =========================
    // ADD INTERACTION - EXISTING MOVIE
    // =========================
    @Test
    void shouldAddInteractionWhenMovieExists() {

        InteractionRequest request = new InteractionRequest();
        request.setUserId(1);
        request.setMovieId(100);
        request.setRating(4);

        User user = new User();
        user.setId(1);

        Movie movie = new Movie();
        movie.setTmdbId(100);
        movie.setTitle("Test Movie");

        when(userRepository.findById(1))
                .thenReturn(Optional.of(user));

        when(movieRepository.findByTmdbId(100))
                .thenReturn(Optional.of(movie));

        UserMovieInteraction saved = new UserMovieInteraction();
        saved.setRating(4);

        when(interactionRepository.save(any()))
                .thenReturn(saved);

        InteractionResponse response =
                service.addInteraction(request);

        assertNotNull(response);
        assertEquals(100, response.getMovieId());
        assertEquals("Test Movie", response.getMovieTitle());
        assertEquals(4, response.getRating());
    }

    // =========================
    // ADD INTERACTION - NEW MOVIE FROM TMDB
    // =========================
    @Test
    void shouldFetchMovieFromTmdbWhenNotInDb() {

        InteractionRequest request = new InteractionRequest();
        request.setUserId(1);
        request.setMovieId(200);
        request.setRating(4);

        User user = new User();
        user.setId(1);

        when(userRepository.findById(1))
                .thenReturn(Optional.of(user));

        when(movieRepository.findByTmdbId(200))
                .thenReturn(Optional.empty());

        Map<String, Object> tmdbMovie = new HashMap<>();
        tmdbMovie.put("id", 200);
        tmdbMovie.put("title", "TMDB Movie");

        Movie mappedMovie = new Movie();
        mappedMovie.setTmdbId(200);
        mappedMovie.setTitle("TMDB Movie");

        when(tmdbService.getMovie(200))
                .thenReturn(tmdbMovie);

        when(tmdbService.mapToEntity(tmdbMovie))
                .thenReturn(mappedMovie);

        when(movieRepository.save(any()))
                .thenReturn(mappedMovie);

        when(interactionRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response =
                service.addInteraction(request);

        assertEquals(200, response.getMovieId());
        assertEquals("TMDB Movie", response.getMovieTitle());
    }

    // =========================
    // UPDATE INTERACTION
    // =========================
    @Test
    void shouldUpdateInteraction() {

        InteractionRequest request = new InteractionRequest();
        request.setUserId(1);
        request.setMovieId(100);
        request.setRating(4);

        User user = new User();
        user.setId(1);

        Movie movie = new Movie();
        movie.setTmdbId(100);
        movie.setTitle("Movie");

        UserMovieInteraction interaction = new UserMovieInteraction();
        interaction.setRating(3);

        when(userRepository.findById(1))
                .thenReturn(Optional.of(user));

        when(movieRepository.findByTmdbId(100))
                .thenReturn(Optional.of(movie));

        when(interactionRepository.findByUserAndMovie(user, movie))
                .thenReturn(Optional.of(interaction));

        when(interactionRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        InteractionResponse response =
                service.updateInteraction(request);

        assertEquals(4, response.getRating());
    }

    // =========================
    // GET USER RATINGS
    // =========================
    @Test
    void shouldReturnUserRatings() {

        User user = new User();
        user.setId(1);

        Movie movie = new Movie();
        movie.setTmdbId(100);
        movie.setTitle("Movie");
        movie.setOverview("Desc");
        movie.setPoster_path("/x.jpg");
        movie.setRelease_date(java.time.LocalDate.of(2020,1,1));

        Person p = new Person();
        p.setName("Actor");

        Genre g = new Genre();
        g.setName("Action");

        movie.setPeople(List.of(p));
        movie.setGenres(List.of(g));

        UserMovieInteraction interaction = new UserMovieInteraction();
        interaction.setUser(user);
        interaction.setMovie(movie);
        interaction.setRating(4);

        when(interactionRepository.findByUserId(1))
                .thenReturn(List.of(interaction));

        List<MovieDto> result =
                service.getUserRatings(1);

        assertEquals(1, result.size());

        MovieDto dto = result.get(0);

        assertEquals(100, dto.getId());
        assertEquals("Movie", dto.getTitle());
        assertEquals(4, dto.getUserRating());
        assertTrue(dto.getCast().contains("Actor"));
        assertTrue(dto.getGenres().contains("Action"));
    }
}

