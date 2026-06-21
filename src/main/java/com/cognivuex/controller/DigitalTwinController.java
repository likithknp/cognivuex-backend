package com.cognivuex.controller;

import com.cognivuex.dto.DigitalTwinResponseDTO;
import com.cognivuex.entity.HealthReport;
import com.cognivuex.repository.HealthReportRepository;
import com.cognivuex.service.DigitalTwinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/digital-twin")
@CrossOrigin("*")
public class DigitalTwinController {

    private final HealthReportRepository repository;

    public DigitalTwinController(
            HealthReportRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping("/latest")
    public ResponseEntity<HealthReport> getLatestTwin() {

        return repository
                .findTopByOrderByIdDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}