package com.mt.project.Controller;

import com.mt.project.Dto.MovieRecommendationRequest;
import com.mt.project.Service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class MovieRecommendationController {
    private final RecommendationService recommendationService;

    public MovieRecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/similar")
    public ResponseEntity<?> getSimilarMovies(@RequestBody MovieRecommendationRequest request) {
        if (request.getMovieTitles() == null || request.getMovieTitles().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "movieTitles cannot be empty"));
        }

        List<Map<String, Object>> movies =
                recommendationService.recommendMovies(request.getMovieTitles());

        return ResponseEntity.ok(Map.of(
                "inputMovies", request.getMovieTitles(),
                "totalResults", movies.size(),
                "movies", movies
        ));
    }
}
