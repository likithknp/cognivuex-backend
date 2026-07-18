package com.cognivuex.service;

import com.cognivuex.dto.ConversationMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT = """
You are CognivueX AI - a professional AI Health Assistant.

Your role:
- Explain health reports in simple, easy-to-understand language
- Use the uploaded health report as your primary information source
- Always maintain a professional and empathetic tone

Guidelines:
- NEVER diagnose diseases
- NEVER prescribe medicines
- NEVER recommend specific medical treatments
- DO explain what abnormal values mean
- DO mention normal ranges for health metrics
- DO suggest healthy foods
- DO recommend exercises appropriate for the patient
- DO suggest lifestyle improvements
- DO advise when professional medical consultation is needed

Response Format:
When analyzing a report, structure your response with these sections:
- Summary: Quick overview of the report
- Abnormal Findings: List any values outside normal range
- Health Risks: Potential concerns based on the data
- Recommendations: Actionable advice
- Foods to Eat: Healthy nutrition suggestions
- Foods to Avoid: Foods that may worsen conditions
- Exercise Suggestions: Appropriate physical activities
- Lifestyle Changes: Sleep, stress management, etc.
- When to Consult a Doctor: Red flags requiring professional help

IMPORTANT DISCLAIMER:
"This analysis is for educational purposes only and is not a substitute for professional medical advice. Always consult with a qualified healthcare provider for medical concerns."
""";

    /**
     * Analyze a health report using OpenAI ChatGPT with conversation history
     */
    public String analyzeHealthReport(String reportText, String question, List<ConversationMessage> conversationHistory, String patientName) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("OpenAI API key is not configured");
            }

            // Build messages list
            List<Map<String, String>> messages = new ArrayList<>();

            // Add system message
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

            // Add context about the report
            String reportContext = buildReportContext(reportText, patientName);
            messages.add(Map.of("role", "system", "content", reportContext));

            // Add conversation history
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                for (ConversationMessage msg : conversationHistory) {
                    messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
                }
            }

            // Add current question
            messages.add(Map.of("role", "user", "content", question));

            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            // Send request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(OPENAI_API_URL, entity, String.class);

            // Parse response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String content = jsonNode.path("choices").path(0).path("message").path("content").asText();
                
                if (content != null && !content.isEmpty()) {
                    log.info("OpenAI API call successful");
                    return content;
                }
            }

            throw new RuntimeException("Invalid response from OpenAI API");

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to analyze health report: " + e.getMessage(), e);
        }
    }

    /**
     * Simpler overload without conversation history
     */
    public String analyzeHealthReport(String reportText, String question, String patientName) {
        return analyzeHealthReport(reportText, question, new ArrayList<>(), patientName);
    }

    /**
     * Build context from report for the AI
     */
    private String buildReportContext(String reportText, String patientName) {
        StringBuilder context = new StringBuilder();

        if (patientName != null && !patientName.isBlank()) {
            context.append(String.format("Patient Name: %s\n\n", patientName));
        }

        context.append("HEALTH REPORT DATA:\n");
        context.append("==================\n");
        context.append(reportText);
        context.append("\n\nBased on the above health report data, please provide analysis and recommendations.");

        return context.toString();
    }
}
