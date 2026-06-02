package com.mt.project.ServiceTest;

import com.mt.project.Dto.KeywordSearchResponse;
import com.mt.project.Service.KeywordService;
import opennlp.tools.postag.POSTaggerME;
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
class KeywordServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private POSTaggerME posTagger;

    @InjectMocks
    private KeywordService keywordService;

    @Test
    void shouldReturnKeywordResponseWhenFound() {

        String keyword = "space";

        Map<String, Object> apiResponse = Map.of(
                "results", List.of(
                        Map.of(
                                "id", 123,
                                "name", "space"
                        )
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(apiResponse);

        KeywordSearchResponse response =
                keywordService.searchKeyword(keyword);

        assertNotNull(response);
        assertEquals(123, response.getId());
        assertEquals("space", response.getName());

        verify(restTemplate, atLeastOnce())
                .getForObject(anyString(), eq(Map.class));
    }

    @Test
    void shouldThrowExceptionWhenKeywordEmpty() {

        assertThrows(
                IllegalArgumentException.class,
                () -> keywordService.searchKeyword("")
        );

        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldThrowExceptionWhenApiReturnsNull() {

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(null);

        assertThrows(
                RuntimeException.class,
                () -> keywordService.searchKeyword("space")
        );
    }
}
