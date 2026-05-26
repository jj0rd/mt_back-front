package com.mt.project.Service;

import com.mt.project.Model.Movie;
import com.mt.project.Repository.MovieRepository;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class LuceneIndexService {
    private final Directory directory;
    private final StandardAnalyzer analyzer;
    private final TmdbService tmdbService;
    private final FeatureExtractionService featureExtractionService;


    public LuceneIndexService(Directory directory, StandardAnalyzer analyzer, TmdbService tmdbService,
                              FeatureExtractionService featureExtractionService) {
        this.directory = directory;
        this.analyzer = analyzer;
        this.tmdbService = tmdbService;
        this.featureExtractionService = featureExtractionService;
    }

    private IndexWriter createWriter() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        return new IndexWriter(directory, config);
    }


    public void indexTmdbMovies(List<Map<String, Object>> movies) {

        try (IndexWriter writer = createWriter()) {

            for (Map<String, Object> movie : movies) {

                Integer tmdbId = (Integer) movie.get("id");

                Map<String, Object> details = tmdbService.getMovie(tmdbId);
                if (details == null) continue;

                List<String> features =
                        featureExtractionService.extractFeaturesFromTmdb(details);

                String content = String.join(" ", features);

                Document doc = new Document();
                doc.add(new StringField("id", tmdbId.toString(), Field.Store.YES));
                doc.add(new TextField("content", content, Field.Store.NO));

                writer.updateDocument(
                        new Term("id", tmdbId.toString()),
                        doc
                );
            }

            writer.commit();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addDocument(Document doc) {

        try (IndexWriter writer = createWriter()) {
            writer.addDocument(doc);
            writer.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean indexExists() {
        try {
            return DirectoryReader.indexExists(directory);
        } catch (Exception e) {
            return false;
        }
    }

}
