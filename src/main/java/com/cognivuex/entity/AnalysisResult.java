package com.cognivuex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Analysis_Result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue
    private Long id;

    private Integer wellnessScore;
    private Double biologicalAge;
    private Integer longevityIndex;

    @Column(length = 5000)
    private String recommendations;

    @Column(length = 5000)
    private String diseaseRisks;

    private LocalDateTime createdAt;
}