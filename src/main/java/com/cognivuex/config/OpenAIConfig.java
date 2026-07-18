package com.cognivuex.config;

import com.openai.client.OpenAIClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Bean
    public OpenAIClient openAIClient() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException(
                "OpenAI API key is not configured. " +
                "Please set 'openai.api.key' property or 'OPENAI_API_KEY' environment variable."
            );
        }
        // Use constructor if available
        try {
            return new OpenAIClient(apiKey);
        } catch (Exception e) {
            // If constructor fails, try another approach
            throw new RuntimeException("Failed to initialize OpenAI client", e);
        }
    }
}
