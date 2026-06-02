package com.mt.project.ControllerTest;

import com.mt.project.Controller.KeywordController;
import com.mt.project.Dto.KeywordSearchRequest;
import com.mt.project.Dto.KeywordSearchResponse;
import com.mt.project.Service.KeywordService;
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
class KeywordControllerTest {

    @Mock
    private KeywordService keywordService;

    @InjectMocks
    private KeywordController controller;

    @Test
    void shouldReturnKeywordWhenFound() {

        KeywordSearchRequest request =
                new KeywordSearchRequest();

        request.setName("space");

        KeywordSearchResponse response =
                new KeywordSearchResponse();

        response.setId(123);
        response.setName("space");

        when(keywordService.searchKeyword("space"))
                .thenReturn(response);

        ResponseEntity<?> result =
                controller.searchKeyword(request);

        assertEquals(200, result.getStatusCode().value());

        KeywordSearchResponse body =
                (KeywordSearchResponse) result.getBody();

        assertNotNull(body);
        assertEquals(123, body.getId());
        assertEquals("space", body.getName());

        verify(keywordService).searchKeyword("space");
    }

    @Test
    void shouldReturnBadRequestWhenKeywordNameIsInvalid() {

        KeywordSearchRequest request =
                new KeywordSearchRequest();

        request.setName("");

        when(keywordService.searchKeyword(""))
                .thenThrow(
                        new IllegalArgumentException(
                                "Keyword name cannot be empty"
                        )
                );

        ResponseEntity<?> result =
                controller.searchKeyword(request);

        assertEquals(400, result.getStatusCode().value());

        Map<?, ?> body =
                (Map<?, ?>) result.getBody();

        assertEquals(
                "Keyword name cannot be empty",
                body.get("error")
        );
    }

    @Test
    void shouldReturn404WhenKeywordNotFound() {

        KeywordSearchRequest request =
                new KeywordSearchRequest();

        request.setName("unknown");

        when(keywordService.searchKeyword("unknown"))
                .thenThrow(
                        new RuntimeException(
                                "Keyword not found"
                        )
                );

        ResponseEntity<?> result =
                controller.searchKeyword(request);

        assertEquals(404, result.getStatusCode().value());

        Map<?, ?> body =
                (Map<?, ?>) result.getBody();

        assertEquals(
                "Keyword not found",
                body.get("error")
        );
    }

    @Test
    void shouldExtractKeywordsFromDescription() {

        String description =
                "A space adventure with aliens";

        Map<String, String> request =
                Map.of("description", description);

        Map<String, Object> serviceResponse =
                Map.of(
                        "description", description,
                        "keywords",
                        List.of(
                                Map.of(
                                        "id", 123,
                                        "name", "space"
                                )
                        )
                );

        when(
                keywordService.extractKeywordsWithNames(
                        description
                )
        ).thenReturn(serviceResponse);

        ResponseEntity<?> result =
                controller.extractKeywords(request);

        assertEquals(200, result.getStatusCode().value());

        assertEquals(
                serviceResponse,
                result.getBody()
        );

        verify(keywordService)
                .extractKeywordsWithNames(description);
    }

    @Test
    void shouldReturnBadRequestForEmptyDescription() {

        Map<String, String> request =
                Map.of("description", "");

        ResponseEntity<?> result =
                controller.extractKeywords(request);

        assertEquals(400, result.getStatusCode().value());

        Map<?, ?> body =
                (Map<?, ?>) result.getBody();

        assertEquals(
                "Description cannot be empty",
                body.get("error")
        );

        verifyNoInteractions(keywordService);
    }

    @Test
    void shouldReturnBadRequestForMissingDescription() {

        Map<String, String> request =
                Map.of();

        ResponseEntity<?> result =
                controller.extractKeywords(request);

        assertEquals(400, result.getStatusCode().value());

        Map<?, ?> body =
                (Map<?, ?>) result.getBody();

        assertEquals(
                "Description cannot be empty",
                body.get("error")
        );

        verifyNoInteractions(keywordService);
    }
}
