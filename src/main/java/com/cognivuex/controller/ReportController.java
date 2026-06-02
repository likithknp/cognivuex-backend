package com.cognivuex.controller;

import com.cognivuex.entity.HealthReport;
import com.cognivuex.repository.HealthReportRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class ReportController {

    private final HealthReportRepository repository;

    public ReportController(
            HealthReportRepository repository
    ) {

        this.repository = repository;
    }

    @PostMapping
    public HealthReport saveReport(
            @RequestBody HealthReport report
    ) {

        return repository.save(report);
    }

    @GetMapping
    public List<HealthReport> getReports() {

        return repository.findAll();
    }
}