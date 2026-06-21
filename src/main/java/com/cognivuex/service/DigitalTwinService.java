package com.cognivuex.service;

import com.cognivuex.dto.DigitalTwinResponseDTO;
import com.cognivuex.entity.HealthReport;
import com.cognivuex.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DigitalTwinService {

    private final HealthReportRepository repository;

    public DigitalTwinResponseDTO getLatestTwin() {

        HealthReport report =
                repository.findTopByOrderByIdDesc()
                        .orElseThrow();

        return DigitalTwinResponseDTO.builder()
                .biologicalAge(report.getBiologicalAge())
                .healthSpanPrediction(report.getHealthSpanPrediction())
                .riskScore(report.getRiskScore())
                .riskLevel(report.getRiskLevel().name())
                .longevityIndex(report.getLongevityIndex())
                .twinAccuracy(report.getTwinAccuracy())

                .cardiovascularScore(report.getCardiovascularScore())
                .immuneScore(report.getImmuneScore())
                .metabolicScore(report.getMetabolicScore())
                .respiratoryScore(report.getRespiratoryScore())
                .endocrineScore(report.getEndocrineScore())
                .nervousSystemScore(report.getNervousSystemScore())

                .wellnessScore(report.getWellnessScore())
                .sleepScore(report.getSleepScore())
                .heartScore(report.getHeartScore())
                .stressScore(report.getStressScore())
                .recoveryScore(report.getRecoveryScore())

                .diseaseRisks(report.getDiseaseRisks())
                .suggestions(report.getSuggestions())

                .build();
    }
}