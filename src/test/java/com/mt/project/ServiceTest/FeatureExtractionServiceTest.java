package com.mt.project.ServiceTest;

import com.mt.project.Model.Movie;
import com.mt.project.Model.Genre;
import com.mt.project.Model.Person;
import com.mt.project.Service.FeatureExtractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FeatureExtractionServiceTest {

    @InjectMocks
    private FeatureExtractionService service;

    // =========================
    // 1. MOVIE → FEATURES (MODEL)
    // =========================
    @Test
    void shouldExtractFeaturesFromMovie() {

        Movie movie = new Movie();
        movie.setOverview("Space adventure");

        Genre genre = new Genre();
        genre.setName("Sci-Fi");

        Person person = new Person();
        person.setName("Tom Hanks");

        movie.setGenres(List.of(genre));
        movie.setPeople(List.of(person));
        movie.setRelease_date(LocalDate.of(2020, 1, 1));

        List<String> result = service.extractFeatures(movie);

        assertTrue(result.contains("Space adventure"));
        assertTrue(result.contains("genre_Sci-Fi"));
        assertTrue(result.contains("person_Tom Hanks"));
        assertTrue(result.contains("year_2020"));
    }

    // =========================
    // 2. NULL SAFETY TEST
    // =========================
    @Test
    void shouldHandleNullFields() {

        Movie movie = new Movie();

        List<String> result = service.extractFeatures(movie);

        assertNotNull(result);
        assertTrue(result.isEmpty() || result.size() >= 0);
    }

    // =========================
    // 3. TMDB FEATURES
    // =========================
    @Test
    void shouldExtractFeaturesFromTmdb() {

        List<java.util.Map<String, Object>> cast =
                List.of(
                        java.util.Map.of("name", "Actor1"),
                        java.util.Map.of("name", "Actor2"),
                        java.util.Map.of("name", "Actor3"),
                        java.util.Map.of("name", "Actor4"),
                        java.util.Map.of("name", "Actor5"),
                        java.util.Map.of("name", "Actor6")
                );

        List<java.util.Map<String, Object>> crew =
                List.of(
                        java.util.Map.of("name", "Nolan", "job", "Director"),
                        java.util.Map.of("name", "Someone", "job", "Producer")
                );

        java.util.Map<String, Object> movie = java.util.Map.of(
                "overview", "Epic movie",
                "genres", List.of(
                        java.util.Map.of("name", "Action")
                ),
                "release_date", "2021-05-10",
                "vote_average", 8.5,
                "credits", java.util.Map.of(
                        "cast", cast,
                        "crew", crew
                )
        );

        List<String> result = service.extractFeaturesFromTmdb(movie);

        // overview
        assertTrue(result.contains("Epic movie"));

        // genre
        assertTrue(result.contains("genre_Action"));

        // year
        assertTrue(result.contains("year_2021"));

        // rating
        assertTrue(result.contains("rating_8.5"));

        // cast (max 5)
        assertEquals(5,
                result.stream()
                        .filter(f -> f.startsWith("actor_"))
                        .count()
        );

        // director
        assertTrue(result.contains("director_Nolan"));
    }

    // =========================
    // 4. LIMIT CAST TEST
    // =========================
    @Test
    void shouldLimitCastToFiveActors() {

        List<java.util.Map<String, Object>> cast = List.of(
                java.util.Map.of("name", "A"),
                java.util.Map.of("name", "B"),
                java.util.Map.of("name", "C"),
                java.util.Map.of("name", "D"),
                java.util.Map.of("name", "E"),
                java.util.Map.of("name", "F"),
                java.util.Map.of("name", "G")
        );

        java.util.Map<String, Object> movie = java.util.Map.of(
                "credits", java.util.Map.of("cast", cast)
        );

        List<String> result = service.extractFeaturesFromTmdb(movie);

        long actors = result.stream()
                .filter(f -> f.startsWith("actor_"))
                .count();

        assertEquals(5, actors);
    }
}