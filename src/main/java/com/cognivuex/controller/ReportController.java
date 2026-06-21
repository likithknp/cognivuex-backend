package com.cognivuex.controller;

import com.cognivuex.entity.HealthReport;
import com.cognivuex.repository.HealthReportRepository;
import com.cognivuex.service.MedicalAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class ReportController {

    private final HealthReportRepository repository;

    @Autowired
    private MedicalAIService medicalAIService;

    public ReportController(
            HealthReportRepository repository
    ) {

        this.repository = repository;
    }

    @PostMapping
    public HealthReport saveReport(@RequestBody HealthReport report) {

        System.out.println("BIO AGE = " + report.getBiologicalAge());
        System.out.println("TWIN ACCURACY = " + report.getTwinAccuracy());
        System.out.println("HEALTH SPAN = " + report.getHealthSpanPrediction());
        return repository.save(report);
    }

    @GetMapping
    public List<HealthReport> getReports() {

        return repository.findAll();
    }

    @GetMapping("/latest")
    public ResponseEntity<HealthReport> getLatestReport() {

        return repository
                .findTopByOrderByIdDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/test-gemini")
    public String testGemini() throws Exception {
        return medicalAIService.analyze("Patient age 45 glucose 110");
    }
}