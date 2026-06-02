package com.mt.project.Controller;

import com.mt.project.Dto.MovieFuzzyDiscoverRequest;
import com.mt.project.Service.KeywordService;
import com.mt.project.Service.PersonService;
import com.mt.project.Service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class MovieFuzzyController {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Autowired
    private RestTemplate restTemplate;

    private final RecommendationService recommendationService;

    public MovieFuzzyController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }
    @PostMapping("/discover/fuzzy")
    public ResponseEntity<?> discoverFuzzy(@RequestBody MovieFuzzyDiscoverRequest request) {
        return ResponseEntity.ok(recommendationService.discoverFuzzy(request));
    }

}
