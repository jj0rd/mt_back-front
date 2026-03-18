package com.mt.project.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class TranslatorService implements DisposableBean {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl = "https://router.huggingface.co/hf-inference/models/Helsinki-NLP/opus-mt-pl-en";

    @Value("${huggingface.api.token}")
    private String apiToken;

    public TranslatorService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        System.out.println("TranslatorService zainicjalizowany z API Hugging Face (nowy router)");
        System.out.println("Używany model: Helsinki-NLP/opus-mt-pl-en");
    }

    public String translate(String polishText) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // WAŻNE: Ustawiamy akceptowalny typ odpowiedzi
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.set("Authorization", "Bearer " + apiToken);

            String requestBody = String.format("{\"inputs\": \"%s\"}",
                    polishText.replace("\"", "\\\"").replace("\n", " "));

            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return parseResponse(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return "Błąd tłumaczenia: " + e.getMessage();
        }
    }

    private String parseResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                if (first.has("translation_text")) {
                    return first.get("translation_text").asText();
                }
            }
            return "Nie można sparsować odpowiedzi: " + jsonResponse;
        } catch (Exception e) {
            return "Błąd parsowania: " + e.getMessage();
        }
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("TranslatorService: zamykanie...");
    }
}