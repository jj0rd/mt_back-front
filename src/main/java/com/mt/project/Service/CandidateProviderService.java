package com.mt.project.Service;

import org.springframework.stereotype.Service;

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

    public void loadCandidatesToIndex() {

        List<Map<String, Object>> candidates =
                tmdbService.getPopularMovies();

        indexService.indexTmdbMovies(candidates);
    }
}
