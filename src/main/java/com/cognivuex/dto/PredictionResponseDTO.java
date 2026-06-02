package com.cognivuex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PredictionResponseDTO {

    private Integer riskScore;

    private String riskLevel;

    private String aiRecommendation;
}