package com.mt.project.Service;

import com.mt.project.Model.Movie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TmdbService {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getMovie(Integer movieId) {

        String url = tmdbApiUrl + "/movie/" + movieId
                + "?api_key=" + tmdbApiKey
                + "&append_to_response=credits,keywords";

        return restTemplate.getForObject(url, Map.class);
    }

    public Movie mapToEntity(Map<String, Object> tmdb) {

        Movie movie = new Movie();

        movie.setTmdbId((Integer) tmdb.get("id"));
        movie.setTitle((String) tmdb.get("title"));
        movie.setOverview((String) tmdb.get("overview"));
        movie.setPoster_path((String) tmdb.get("poster_path"));

        // 📅 data
        String date = (String) tmdb.get("release_date");
        if (date != null && !date.isEmpty()) {
            movie.setRelease_date(java.time.LocalDate.parse(date));
        }

        // 🎭 genres
        List<Map<String, Object>> genres = (List<Map<String, Object>>) tmdb.get("genres");
        if (genres != null) {
            movie.setGenres(
                    genres.stream()
                            .map(g -> ((String) g.get("name")).toLowerCase())
                            .toList()
            );
        }

        // 🔑 keywords
        Map<String, Object> keywordsMap = (Map<String, Object>) tmdb.get("keywords");
        if (keywordsMap != null) {
            List<Map<String, Object>> keywords = (List<Map<String, Object>>) keywordsMap.get("keywords");

            movie.setKeywords(
                    keywords.stream()
                            .map(k -> ((String) k.get("name")).toLowerCase().replace(" ", "_"))
                            .toList()
            );
        }

        // 🎬 cast (top 5)
        Map<String, Object> credits = (Map<String, Object>) tmdb.get("credits");
        if (credits != null) {
            List<Map<String, Object>> cast = (List<Map<String, Object>>) credits.get("cast");

            movie.setPeople(
                    cast.stream()
                            .limit(5)
                            .map(c -> ((String) c.get("name")).toLowerCase().replace(" ", "_"))
                            .toList()
            );
        }

        return movie;
    }

}
