package com.mt.project.ControllerTest;

import com.mt.project.Controller.InteractionController;
import com.mt.project.Dto.InteractionRequest;
import com.mt.project.Dto.InteractionResponse;
import com.mt.project.Dto.MovieDto;
import com.mt.project.Service.UserMovieInteractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionControllerTest {

    @Mock
    private UserMovieInteractionService interactionService;

    @InjectMocks
    private InteractionController controller;

    @Test
    void shouldAddInteraction() {

        InteractionRequest request = new InteractionRequest();
        request.setUserId(1);
        request.setMovieId(100);
        request.setRating(5);

        InteractionResponse response =
                new InteractionResponse();

        response.setMovieId(100);
        response.setMovieTitle("Matrix");
        response.setRating(5);

        when(interactionService.addInteraction(request))
                .thenReturn(response);

        InteractionResponse result =
                controller.addInteraction(request);

        assertEquals(100, result.getMovieId());
        assertEquals("Matrix", result.getMovieTitle());
        assertEquals(5, result.getRating());

        verify(interactionService).addInteraction(request);
    }

    @Test
    void shouldUpdateInteraction() {

        InteractionRequest request = new InteractionRequest();
        request.setUserId(1);
        request.setMovieId(100);
        request.setRating(4);

        InteractionResponse response =
                new InteractionResponse();

        response.setMovieId(100);
        response.setMovieTitle("Matrix");
        response.setRating(4);

        when(interactionService.updateInteraction(request))
                .thenReturn(response);

        InteractionResponse result =
                controller.updateInteraction(request);

        assertEquals(4, result.getRating());

        verify(interactionService).updateInteraction(request);
    }

    @Test
    void shouldReturnUserRatings() {

        MovieDto movieDto = new MovieDto();

        movieDto.setId(100);
        movieDto.setTitle("Matrix");
        movieDto.setUserRating(5);

        List<MovieDto> expected =
                List.of(movieDto);

        when(interactionService.getUserRatings(1))
                .thenReturn(expected);

        List<MovieDto> result =
                controller.getUserRatings(1);

        assertEquals(1, result.size());
        assertEquals("Matrix", result.get(0).getTitle());
        assertEquals(5, result.get(0).getUserRating());

        verify(interactionService).getUserRatings(1);
    }
}
