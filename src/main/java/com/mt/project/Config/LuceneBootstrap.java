//package com.mt.project.Config;
//
//import com.mt.project.Service.CandidateProviderService;
//import com.mt.project.Service.LuceneIndexService;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.nio.file.Paths;
//
//@Component
//public class LuceneBootstrap {
//    private final LuceneIndexService luceneIndexService;
//    private final CandidateProviderService candidateProviderService;
//
//    public LuceneBootstrap(LuceneIndexService luceneIndexService, CandidateProviderService candidateProviderService) {
//        this.luceneIndexService = luceneIndexService;
//        this.candidateProviderService = candidateProviderService;
//    }
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void init() {
//        candidateProviderService.loadCandidatesToIndex();
//    }
//
//    @Bean
//    public Directory directory() throws IOException {
//        return FSDirectory.open(Paths.get("lucene-index"));
//    }
//
//    @Bean
//    public StandardAnalyzer analyzer() {
//        return new StandardAnalyzer();
//    }
//}
