package com.mt.project.Config;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class LuceneConfig {
    @Bean
    public Directory luceneDirectory() throws Exception {
        return FSDirectory.open(Paths.get("lucene-index"));
    }

    @Bean
    public StandardAnalyzer analyzer() {
        return new StandardAnalyzer();
    }


}
