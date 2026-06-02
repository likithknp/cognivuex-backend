package com.cognivuex.service;

import com.cognivuex.dto.PredictionResponseDTO;
import com.cognivuex.entity.HealthReport;

public interface PredictionService {

    PredictionResponseDTO analyze(HealthReport report);
}