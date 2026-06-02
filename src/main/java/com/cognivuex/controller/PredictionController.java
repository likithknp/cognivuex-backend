package com.cognivuex.controller;

import com.cognivuex.dto.PredictionResponseDTO;
import com.cognivuex.entity.HealthReport;
import com.cognivuex.service.PredictionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predict")
@CrossOrigin("*")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(
            PredictionService predictionService
    ) {

        this.predictionService = predictionService;
    }

    @PostMapping
    public PredictionResponseDTO predict(
            @RequestBody HealthReport report
    ) {

        return predictionService.analyze(report);
    }
}