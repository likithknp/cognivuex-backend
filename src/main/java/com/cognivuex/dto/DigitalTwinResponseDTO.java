package com.cognivuex.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalTwinResponseDTO {

    private Double biologicalAge;

    private Integer healthSpanPrediction;

    private Integer riskScore;

    private String riskLevel;

    private Integer longevityIndex;

    private Integer twinAccuracy;

    private Integer cardiovascularScore;

    private Integer immuneScore;

    private Integer metabolicScore;

    private Integer respiratoryScore;

    private Integer endocrineScore;

    private Integer nervousSystemScore;

    private Integer wellnessScore;

    private Integer sleepScore;

    private Integer heartScore;

    private Integer stressScore;

    private Integer recoveryScore;

    private String diseaseRisks;

    private String suggestions;
}