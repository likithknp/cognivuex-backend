package com.cognivuex.entity;

import com.cognivuex.entity.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

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

    private String patientName;
    private Integer age;
    private Integer sugar;
    private Integer glucose;
    private Integer cholesterol;
    private Integer systolicBP;
    private Integer diastolicBP;
    private Double bmi;
    private Integer heartRate;
    private Double hba1c;
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(length = 2000)
    private String suggestions;

    @Column(length = 5000)
    private String extractedText;

    private String uploadedFileName;
}