package com.cognivuex.service;

import com.cognivuex.dto.ConversationMessage;
import com.openai.client.OpenAIClient;
import com.openai.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIService.class);

    private final OpenAIClient openAIClient;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    public OpenAIService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

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
            List<ChatCompletionMessageParam> messages = new ArrayList<>();

            // Add system message
            messages.add(ChatCompletionMessageParam.ofSystem(SYSTEM_PROMPT));

            // Add context about the report
            String reportContext = buildReportContext(reportText, patientName);
            messages.add(ChatCompletionMessageParam.ofSystem(reportContext));

            // Add conversation history
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                for (ConversationMessage msg : conversationHistory) {
                    if ("user".equalsIgnoreCase(msg.getRole())) {
                        messages.add(ChatCompletionMessageParam.ofUser(msg.getContent()));
                    } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                        messages.add(ChatCompletionMessageParam.ofAssistant(msg.getContent()));
                    }
                }
            }

            // Add current question
            messages.add(ChatCompletionMessageParam.ofUser(question));

            // Call OpenAI API
            ChatCompletion completion = openAIClient.chat().completions().create(ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(2000)
                    .build());

            String response = completion.choices().get(0).message().content().orElse(
                "Unable to generate response. Please try again.");

            log.info("OpenAI API call successful");
            return response;

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
