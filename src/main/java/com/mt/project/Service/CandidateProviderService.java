package com.mt.project.Service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CandidateProviderService {
    private final TmdbService tmdbService;
    private final LuceneIndexService indexService;

    public CandidateProviderService(TmdbService tmdbService,
                                    LuceneIndexService indexService) {
        this.tmdbService = tmdbService;
        this.indexService = indexService;
    }

//    public void loadCandidatesToIndex() {
//
//        List<Map<String, Object>> candidates =
//                tmdbService.getPopularMovies();
//
//        indexService.indexTmdbMovies(candidates);
//    }
    public void loadCandidatesToIndex() {

        if (indexService.indexExists()) {
            System.out.println("Index already exists → skipping rebuild");
            return;
        }

        int totalIndexed = 0;

        for (int page = 1; page <= 200; page++) {

            try {
                List<Map<String, Object>> movies =
                        tmdbService.discoverMovies(page);

                if (movies == null || movies.isEmpty()) {
                    break;
                }

                indexService.indexTmdbMovies(movies);

                totalIndexed += movies.size();

                System.out.println("Indexed page: " + page
                        + " | total: " + totalIndexed);

                // optional: avoid TMDB rate limit
                Thread.sleep(200);

            } catch (Exception e) {
                System.err.println("Error on page " + page + ": " + e.getMessage());
            }
        }

        System.out.println("DONE. Total indexed: " + totalIndexed);
    }
}
