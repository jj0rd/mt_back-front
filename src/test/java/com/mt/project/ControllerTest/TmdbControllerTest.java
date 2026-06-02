package com.mt.project.ControllerTest;

import com.mt.project.Controller.TmdbController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TmdbController controller;

    // =========================
    // 1. SUCCESS CASE
    // =========================
    @Test
    void shouldReturnMoviesWhenFound() {

        Map<String, Object> tmdbResponse = Map.of(
                "results", List.of(
                        Map.of("id", 1, "title", "Matrix"),
                        Map.of("id", 2, "title", "Matrix Reloaded")
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(tmdbResponse);

        ResponseEntity<?> result =
                controller.searchMovie("Matrix", "en-US");

        assertEquals(200, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("Matrix", body.get("query"));
        assertEquals("en-US", body.get("language"));
        assertEquals(2, body.get("totalResults"));
    }

    // =========================
    // 2. EMPTY TITLE
    // =========================
    @Test
    void shouldReturnBadRequestWhenTitleIsEmpty() {

        ResponseEntity<?> result =
                controller.searchMovie("   ", "en-US");

        assertEquals(400, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("Title cannot be empty", body.get("Error"));
    }

    // =========================
    // 3. NULL RESPONSE FROM TMDB
    // =========================
    @Test
    void shouldReturn404WhenTmdbReturnsNull() {

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(null);

        ResponseEntity<?> result =
                controller.searchMovie("Matrix", "en-US");

        assertEquals(404, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("Movie not found!", body.get("Error"));
    }

    // =========================
    // 4. MISSING RESULTS FIELD
    // =========================
    @Test
    void shouldReturn404WhenNoResultsKey() {

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("wrongKey", "value"));

        ResponseEntity<?> result =
                controller.searchMovie("Matrix", "en-US");

        assertEquals(404, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("Movie not found!", body.get("Error"));
    }

    // =========================
    // 5. DEFAULT LANGUAGE PARAM
    // =========================
    @Test
    void shouldUseDefaultLanguageWhenNotProvided() {

        Map<String, Object> tmdbResponse = Map.of(
                "results", List.of(
                        Map.of("id", 1, "title", "Matrix")
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(tmdbResponse);

        ResponseEntity<?> result =
                controller.searchMovie("Matrix", "en-US");

        assertEquals(200, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("en-US", body.get("language"));
    }
}
