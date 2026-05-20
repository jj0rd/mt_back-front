package com.mt.project;

import com.mt.project.Service.CandidateProviderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class ProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectApplication.class, args);
	}

//	@Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }

	@Bean
	CommandLineRunner init(CandidateProviderService candidateService) {
		return args -> {
			candidateService.loadCandidatesToIndex();
		};
	}

	@Bean
	public WebClient webClient() {
		return WebClient.builder().build();
	}
}
