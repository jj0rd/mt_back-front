package com.mt.project.ServiceTest;

import com.mt.project.Dto.PersonSearchResponse;
import com.mt.project.Service.PersonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PersonService personService;

    // =========================
    // getPersonIdByName - success
    // =========================
    @Test
    void shouldReturnPersonIdByName() {

        Map<String, Object> apiResponse = Map.of(
                "results", List.of(
                        Map.of(
                                "id", 123,
                                "name", "Tom Hanks"
                        )
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        Integer result =
                personService.getPersonIdByName("Tom Hanks");

        assertEquals(123, result);
    }

    // =========================
    // getPersonIdByName - empty name
    // =========================
    @Test
    void shouldReturnNullWhenNameIsBlank() {

        Integer result =
                personService.getPersonIdByName("   ");

        assertNull(result);

        verifyNoInteractions(restTemplate);
    }

    // =========================
    // getPersonIdByName - no results
    // =========================
    @Test
    void shouldReturnNullWhenNoResults() {

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));

        Integer result =
                personService.getPersonIdByName("Unknown");

        assertNull(result);
    }

    // =========================
    // searchPerson - success
    // =========================
    @Test
    void shouldSearchPersonSuccessfully() {

        Map<String, Object> apiResponse = Map.of(
                "results", List.of(
                        Map.of(
                                "id", 1,
                                "name", "Leonardo DiCaprio",
                                "known_for", List.of(
                                        Map.of("title", "Inception"),
                                        Map.of("title", "Titanic")
                                )
                        )
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        PersonSearchResponse response =
                personService.searchPerson("Leonardo");

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Leonardo DiCaprio", response.getName());
        assertEquals(2, response.getKnownFor().size());
        assertTrue(response.getKnownFor().contains("Inception"));
    }

    // =========================
    // searchPerson - empty input
    // =========================
    @Test
    void shouldThrowExceptionWhenNameIsEmpty() {

        assertThrows(IllegalArgumentException.class,
                () -> personService.searchPerson("   "));
    }

    // =========================
    // searchPerson - no results
    // =========================
    @Test
    void shouldThrowExceptionWhenPersonNotFound() {

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));

        assertThrows(RuntimeException.class,
                () -> personService.searchPerson("Unknown"));
    }
}
