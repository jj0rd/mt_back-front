package com.mt.project.Service;

import com.mt.project.Model.Movie;
import com.mt.project.Repository.MovieRepository;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class LuceneIndexService {
    private final Directory directory;
    private final StandardAnalyzer analyzer;
    private final MovieRepository movieRepository;
    private final TmdbService tmdbService;
    private final FeatureExtractionService featureExtractionService;
    private final IndexWriter writer;

    public LuceneIndexService(Directory directory, StandardAnalyzer analyzer,
                              MovieRepository movieRepository,TmdbService tmdbService,
                              FeatureExtractionService featureExtractionService) {
        this.directory = directory;
        this.analyzer = analyzer;
        this.movieRepository = movieRepository;
        this.tmdbService = tmdbService;
        this.featureExtractionService = featureExtractionService;

        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            this.writer = new IndexWriter(directory, config);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create Lucene IndexWriter", e);
        }
    }

    public void indexMovie(Movie movie, String content) {

        try {
            Document doc = new Document();

            doc.add(new StringField("id", movie.getTmdbId().toString(), Field.Store.YES));
            doc.add(new TextField("content", content, Field.Store.NO));

            writer.addDocument(doc);

            System.out.println("INDEXING MOVIE: " + movie.getTmdbId());

        } catch (IOException e) {
            throw new RuntimeException("Lucene indexing failed", e);
        }
    }
    public void rebuildIndex() {

        try {
            writer.deleteAll();

            List<Movie> movies = movieRepository.findAll();

            for (Movie movie : movies) {

                Map<String, Object> tmdb = tmdbService.getMovie(movie.getTmdbId());
                if (tmdb == null) continue;

                List<String> features =
                        featureExtractionService.extractFeaturesFromTmdb(tmdb);

                String content = String.join(" ", features);

                indexMovie(movie, content);
            }

            writer.commit(); //  TYLKO RAZ
            writer.close();  //  TYLKO RAZ

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
