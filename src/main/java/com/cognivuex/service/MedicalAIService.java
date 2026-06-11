package com.cognivuex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class MedicalAIService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(MedicalAIService.class);

    public String analyze(String reportText) throws Exception {

        // Fail fast if API key not configured
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("gemini.api.key is not configured. Set gemini.api.key in application.properties or provide a service account token.");
        }

        String prompt = """
                Analyze the following medical report and extract key health metrics.

                Return ONLY valid JSON in this exact format, with no additional text or markdown:

                {
                  "patientName":"",
                  "age":0,
                  "glucose":0,
                  "cholesterol":0,
                  "bmi":0.0,
                  "heartRate":0,
                  "hba1c":0.0,
                  "systolicBP":0,
                  "diastolicBP":0
                }

                If a value is not found in the report, use 0 or empty string.

                Medical Report:
                """ + reportText;

        // Build request body compatible with Generative Language REST endpoint
        Map<String, Object> body = Map.of(
                "contents",
                List.of(
                        Map.of(
                                "parts", List.of(Map.of("text", prompt))
                        )
                )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

        if (apiKey != null && !apiKey.isBlank()) {
            url = url + "?key=" + apiKey;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        }
        catch (HttpClientErrorException | HttpServerErrorException ex) {
            // Log response body and status for easier debugging
            String errorBody = ex.getResponseBodyAsString();
            log.error("Gemini API error: status={} body={}", ex.getStatusCode(), errorBody);
            throw ex;
        }

        String responseBody = response.getBody();

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Gemini API returned non-2xx status: {} body={}", response.getStatusCode(), responseBody);
            throw new IllegalStateException("Gemini API call failed: " + response.getStatusCode());
        }

        if (responseBody == null) return "";

        // Attempt to extract the generated text from common response shapes.
        try {
            JsonNode root = mapper.readTree(responseBody);

            // 1) Check candidates -> candidate.content[*].text
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);

                JsonNode content = candidate.path("content");
                if (content.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode piece : content) {
                        if (piece.has("text")) sb.append(piece.path("text").asText());
                        else if (piece.has("mime_type") && piece.has("text")) sb.append(piece.path("text").asText());
                    }
                    String out = sb.toString().trim();
                    if (!out.isEmpty()) return out;
                }

                // candidate.output
                if (candidate.has("output")) {
                    String out = candidate.path("output").asText();
                    if (!out.isEmpty()) return out;
                }
            }

            // 2) Some responses put text under "output" or "message"
            if (root.has("output")) {
                String out = root.path("output").asText();
                if (!out.isEmpty()) return out;
            }

            if (root.has("message")) {
                String out = root.path("message").asText();
                if (!out.isEmpty()) return out;
            }

            // 3) Fallback: return full body (caller will try to parse it)
            return responseBody;
        }
        catch (Exception e) {
            // If parsing fails, return raw body so caller can handle fallback
            return responseBody;
        }
    }
}

