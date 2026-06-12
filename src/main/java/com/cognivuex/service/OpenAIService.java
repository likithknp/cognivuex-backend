package com.cognivuex.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class OpenAIService {

    @Value("${openai.api.key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String analyzeMedicalReport(String reportText) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Property openai.api.key is not set. Please configure it in application.properties or as an environment variable.");
        }

        RestTemplate restTemplate = new RestTemplate();

        String prompt = """
                Analyze the following medical report.

                Return ONLY JSON.

                {
                  "patientName":"",
                  "age":0,
                  "glucose":0,
                  "cholesterol":0,
                  "bmi":0,
                  "heartRate":0,
                  "hba1c":0,
                  "systolicBP":0,
                  "diastolicBP":0
                }

                Report:
                """ + reportText;

        Map<String, Object> body = Map.of(
                "model", "gpt-4.1-mini",
                "input", prompt
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "https://api.openai.com/v1/responses",
                        HttpMethod.POST,
                        entity,
                        String.class
                );

        return response.getBody();
    }
}