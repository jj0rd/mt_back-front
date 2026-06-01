package com.mt.project.Service;

import com.mt.project.Model.Genre;
import com.mt.project.Model.Keyword;
import com.mt.project.Model.Movie;
import com.mt.project.Model.Person;
import com.mt.project.Repository.GenreRepository;
import com.mt.project.Repository.KeywordRepository;
import com.mt.project.Repository.PersonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class TmdbService {
    @Value("${tmdb.api.url}")
    private String tmdbApiUrl;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    private final RestTemplate restTemplate;
    private final GenreRepository genreRepository;
    private final KeywordRepository keywordRepository;
    private final PersonRepository personRepository;

    public TmdbService(RestTemplate restTemplate,GenreRepository genreRepository,
                       KeywordRepository keywordRepository, PersonRepository personRepository) {
        this.restTemplate = restTemplate;
        this.genreRepository = genreRepository;
        this.keywordRepository = keywordRepository;
        this.personRepository = personRepository;
    }

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

        movie.setVoteAverage((Double) tmdb.get("vote_average"));

        // 📅 data
        String date = (String) tmdb.get("release_date");
        if (date != null && !date.isEmpty()) {
            movie.setRelease_date(java.time.LocalDate.parse(date));
        }

        // 🎭 genres
        List<Map<String, Object>> genres = (List<Map<String, Object>>) tmdb.get("genres");
        if (genres != null) {
            List<Genre> genreEntities = genres.stream()
                    .map(g -> ((String) g.get("name")).toLowerCase())
                    .map(name ->
                            genreRepository.findByName(name)
                                    .orElseGet(() -> {
                                        Genre genre = new Genre();
                                        genre.setName(name);
                                        return genreRepository.save(genre);
                                    })
                    )
                    .toList();

            movie.setGenres(genreEntities);
        }

        // 🔑 keywords
        Map<String, Object> keywordsMap = (Map<String, Object>) tmdb.get("keywords");
        if (keywordsMap != null) {
            List<Map<String, Object>> keywords = (List<Map<String, Object>>) keywordsMap.get("keywords");

            List<Keyword> keywordEntities = keywords.stream()
                    .map(k -> ((String) k.get("name")).toLowerCase().replace(" ", "_"))
                    .map(name ->
                            keywordRepository.findByName(name)
                                    .orElseGet(() -> {
                                        Keyword keyword = new Keyword();
                                        keyword.setName(name);
                                        return keywordRepository.save(keyword);
                                    })
                    )
                    .toList();

            movie.setKeywords(keywordEntities);
        }

        // 🎬 cast (top 5)
        Map<String, Object> credits = (Map<String, Object>) tmdb.get("credits");
        if (credits != null) {
            List<Person> people = new ArrayList<>();

            // 🎭 CAST (top 5 aktorów)
            List<Map<String, Object>> cast =
                    (List<Map<String, Object>>) credits.get("cast");

            if (cast != null) {
                people.addAll(
                        cast.stream()
                                .limit(5)
                                .map(c -> ((String) c.get("name"))
                                        .toLowerCase()
                                        .replace(" ", "_"))
                                .map(name ->
                                        personRepository.findByName(name)
                                                .orElseGet(() -> {
                                                    Person p = new Person();
                                                    p.setName(name);
                                                    return personRepository.save(p);
                                                })
                                )
                                .toList()
                );
            }

            // 🎬 DIRECTOR (NOWE - DODANE)
            List<Map<String, Object>> crew =
                    (List<Map<String, Object>>) credits.get("crew");

            if (crew != null) {
                crew.stream()
                        .filter(c -> "Director".equals(c.get("job")))
                        .map(c -> ((String) c.get("name"))
                                .toLowerCase()
                                .replace(" ", "_"))
                        .forEach(name -> {

                            Person director = personRepository.findByName(name)
                                    .orElseGet(() -> {
                                        Person p = new Person();
                                        p.setName(name);
                                        return personRepository.save(p);
                                    });

                            people.add(director);
                        });
            }

            movie.setPeople(people);
        }

        return movie;
    }

    public List<Map<String, Object>> getPopularMovies() {
        String url = tmdbApiUrl + "/movie/popular?api_key=" + tmdbApiKey;
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>) response.get("results");
    }

    public Map<String, Object> getRandomTopRatedMovie() {

        Random random = new Random();

        // TMDB top_rated ma wiele stron (bezpiecznie np. 1–500)
        int randomPage = random.nextInt(20) + 1;

        String url = tmdbApiUrl + "/movie/top_rated?api_key="
                + tmdbApiKey + "&page=" + randomPage;

        Map<String, Object> response =
                restTemplate.getForObject(url, Map.class);

        if (response == null || response.get("results") == null) {
            throw new RuntimeException("TMDB response is null");
        }

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("results");

        if (results.isEmpty()) {
            throw new RuntimeException("Brak filmów na stronie: " + randomPage);
        }

        // losowy film z tej strony
        return results.get(random.nextInt(results.size()));
    }

    public Map<Integer, Map<String, Object>> getMoviesBatch(List<Integer> ids) {
        return ids.parallelStream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            try {
                                return getMovie(id);
                            } catch (Exception e) {
                                return null;
                            }
                        },
                        (existing, replacement) -> existing
                ));
    }

    public List<Map<String, Object>> discoverMovies(int page) {

        String url = tmdbApiUrl + "/discover/movie"
                + "?api_key=" + tmdbApiKey
                + "&sort_by=popularity.desc"
                + "&vote_count.gte=50"
                + "&page=" + page;

        Map<String, Object> response =
                restTemplate.getForObject(url, Map.class);

        return (List<Map<String, Object>>)
                response.getOrDefault("results", List.of());
    }
}
