package com.mt.project.ControllerTest;

import com.mt.project.Controller.PersonController;
import com.mt.project.Dto.PersonSearchRequest;
import com.mt.project.Dto.PersonSearchResponse;
import com.mt.project.Service.PersonService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private PersonService personService;

    @InjectMocks
    private PersonController controller;

    @Test
    void shouldReturnPersonWhenFound() {

        PersonSearchRequest request = new PersonSearchRequest();
        request.setName("Tom Hanks");

        PersonSearchResponse response = new PersonSearchResponse();
        response.setId(31);
        response.setName("Tom Hanks");
        response.setKnownFor(List.of("Forrest Gump", "Cast Away"));

        when(personService.searchPerson("Tom Hanks"))
                .thenReturn(response);

        ResponseEntity<?> result =
                controller.searchPerson(request);

        assertEquals(200, result.getStatusCode().value());

        PersonSearchResponse body =
                (PersonSearchResponse) result.getBody();

        assertNotNull(body);
        assertEquals(31, body.getId());
        assertEquals("Tom Hanks", body.getName());
        assertEquals(2, body.getKnownFor().size());

        verify(personService).searchPerson("Tom Hanks");
    }

    @Test
    void shouldReturnBadRequestWhenNameIsEmpty() {

        PersonSearchRequest request = new PersonSearchRequest();
        request.setName("");

        when(personService.searchPerson(""))
                .thenThrow(
                        new IllegalArgumentException("Name cannot be empty")
                );

        ResponseEntity<?> result =
                controller.searchPerson(request);

        assertEquals(400, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("Name cannot be empty", body.get("error"));
    }

    @Test
    void shouldReturn404WhenPersonNotFound() {

        PersonSearchRequest request = new PersonSearchRequest();
        request.setName("Unknown");

        when(personService.searchPerson("Unknown"))
                .thenThrow(
                        new RuntimeException("Person not found")
                );

        ResponseEntity<?> result =
                controller.searchPerson(request);

        assertEquals(404, result.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) result.getBody();

        assertEquals("Person not found", body.get("error"));
    }
}
