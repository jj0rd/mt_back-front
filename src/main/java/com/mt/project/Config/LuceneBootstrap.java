package com.mt.project.Config;

import com.mt.project.Service.LuceneIndexService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LuceneBootstrap {
    private final LuceneIndexService luceneIndexService;

    public LuceneBootstrap(LuceneIndexService luceneIndexService) {
        this.luceneIndexService = luceneIndexService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        luceneIndexService.rebuildIndex();
    }
}
