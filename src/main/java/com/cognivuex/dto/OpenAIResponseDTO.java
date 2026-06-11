package com.cognivuex.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIResponseDTO {

    private Integer wellnessScore;
    private Double biologicalAge;
    private Integer longevityIndex;

    private Integer sleepScore;
    private Integer heartScore;
    private Integer stressScore;
    private Integer recoveryScore;

    private List<String> diseaseRisks;
    private List<String> recommendations;

    private String patientName;
    private Integer age;
}