package com.cognivuex.service.impl;

import com.cognivuex.dto.PredictionResponseDTO;
import com.cognivuex.entity.HealthReport;
import com.cognivuex.service.PredictionService;
import org.springframework.stereotype.Service;

@Service
public class PredictionServiceImpl implements PredictionService {

    @Override
    public PredictionResponseDTO analyze(HealthReport report) {

        String text = report.getExtractedText();

        int score = calculateScore(text);

        String level;
        String recommendation;

        if (score < 30) {
            level = "LOW";
            recommendation = "No immediate concern. Maintain regular monitoring.";
        } else if (score < 70) {
            level = "MEDIUM";
            recommendation = "Moderate risk detected. Consider medical consultation.";
        } else {
            level = "HIGH";
            recommendation = "High risk detected. Immediate attention required.";
        }

        return new PredictionResponseDTO(
                score,
                level,
                recommendation
        );
    }

    private int calculateScore(String text) {
        if (text == null) return 0;
        return Math.min(100, text.length() % 100 + 20);
    }
}