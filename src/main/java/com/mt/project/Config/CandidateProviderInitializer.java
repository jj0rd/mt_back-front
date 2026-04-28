package com.mt.project.Config;

import com.mt.project.Service.CandidateProviderService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CandidateProviderInitializer {
    private final CandidateProviderService service;

    public CandidateProviderInitializer(CandidateProviderService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        service.loadCandidatesToIndex();
    }
}
