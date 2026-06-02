package com.mt.project.Controller;

import com.mt.project.Dto.InteractionRequest;
import com.mt.project.Dto.InteractionResponse;
import com.mt.project.Dto.MovieDto;
import com.mt.project.Model.Movie;
import com.mt.project.Model.User;
import com.mt.project.Model.UserMovieInteraction;
import com.mt.project.Repository.MovieRepository;
import com.mt.project.Repository.UserMovieInteractionRepository;
import com.mt.project.Repository.UserRepository;
import com.mt.project.Service.TmdbService;
import com.mt.project.Service.UserMovieInteractionService;
import org.springframework.web.bind.annotation.*;
import com.mt.project.Model.Genre;
import com.mt.project.Model.Person;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interactions")
public class InteractionController {

    private final UserMovieInteractionService interactionService;

    public InteractionController(
            UserMovieInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/add")
    public InteractionResponse addInteraction(
            @RequestBody InteractionRequest request) {

        return interactionService.addInteraction(request);
    }

    @PutMapping("/update")
    public InteractionResponse updateInteraction(
            @RequestBody InteractionRequest request) {

        return interactionService.updateInteraction(request);
    }

    @GetMapping("/{userId}/ratings")
    public List<MovieDto> getUserRatings(
            @PathVariable Integer userId) {

        return interactionService.getUserRatings(userId);
    }
}
