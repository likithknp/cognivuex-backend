package com.cognivuex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "health_seq")
    @SequenceGenerator(
            name = "health_seq",
            sequenceName = "HEALTH_REPORT_SEQ",
            allocationSize = 1
    )
    private Long id;

    // =========================
    // USER RELATION
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // =========================
    // PATIENT DETAILS
    // =========================

    private String patientName;

    private Integer age;

    private String gender;

    // =========================
    // MEDICAL VALUES
    // =========================

    private Integer sugar;

    private Integer glucose;

    private Integer cholesterol;

    private Integer systolicBP;

    private Integer diastolicBP;

    private Double bmi;

    private Integer heartRate;

    private Double hba1c;

    // =========================
    // AI PREDICTION
    // =========================

    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    // =========================
    // AI HEALTH INSIGHTS
    // =========================

    private Integer wellnessScore;

    private Double biologicalAge;

    private Integer longevityIndex;

    private Integer sleepScore;

    private Integer heartScore;

    private Integer stressScore;

    private Integer recoveryScore;

    // =========================
    // AI ANALYSIS
    // =========================

    @Column(length = 5000)
    private String diseaseRisks;

    @Column(length = 5000)
    private String suggestions;

    // =========================
    // REPORT DATA
    // =========================

    @Column(length = 20000)
    private String extractedText;

    private String uploadedFileName;

    // =========================
    // AUDIT FIELDS
    // =========================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // DIGITAL TWIN METRICS

    private Integer twinAccuracy;

    private Integer healthSpanPrediction;

    private Integer cardiovascularScore;

    private Integer immuneScore;

    private Integer metabolicScore;

    private Integer respiratoryScore;

    private Integer endocrineScore;

    private Integer nervousSystemScore;
}