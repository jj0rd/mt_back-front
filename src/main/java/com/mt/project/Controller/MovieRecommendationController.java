package com.mt.project.Controller;

import com.mt.project.Dto.MovieDto;
import com.mt.project.Dto.MovieRecommendationRequest;
import com.mt.project.Model.Movie;
import com.mt.project.Model.ProfileSource;
import com.mt.project.Service.RecommendationService;
import com.mt.project.Service.UserBasedLuceneRecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class MovieRecommendationController {
    private final RecommendationService recommendationService;
    private final UserBasedLuceneRecommendationService luceneRecommendationService;

    public MovieRecommendationController(RecommendationService recommendationService,UserBasedLuceneRecommendationService luceneRecommendationService) {
        this.recommendationService = recommendationService;
        this.luceneRecommendationService = luceneRecommendationService;
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
    // GET /api/recommendations/user/1
    @GetMapping("/luceneRecommend/{userId}")
    public List<MovieDto> getRecommendations(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "TMDB") ProfileSource source) { //TMDB / DATABASE

        // Przekazujemy wybrany source do metody biznesowej
        return luceneRecommendationService.recommend(userId, source);
    }
    @GetMapping("/to-rate/{userId}")
    public ResponseEntity<MovieDto> getMovieToRate(@PathVariable Integer userId) {
        return ResponseEntity.ok(luceneRecommendationService.getMovieToRate(userId));
    }

}
