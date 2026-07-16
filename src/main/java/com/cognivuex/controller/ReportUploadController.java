package com.cognivuex.controller;

import com.cognivuex.entity.HealthReport;
import com.cognivuex.repository.HealthReportRepository;
import com.cognivuex.service.impl.FileAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ReportUploadController {

    private static final Logger log = LoggerFactory.getLogger(ReportUploadController.class);
    private final FileAnalysisService fileAnalysisService;
    private final HealthReportRepository reportRepository;

    public ReportUploadController(
            FileAnalysisService fileAnalysisService,
            HealthReportRepository reportRepository
    ) {
        this.fileAnalysisService = fileAnalysisService;
        this.reportRepository = reportRepository;
    }

    /**
     * Upload a health report file (PDF, DOCX, image)
     * Extracts text, analyzes it, and saves to database
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadReport(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            log.info("Upload request for file: {}", file.getOriginalFilename());
            
            // Process the file (extract text, analyze, save)
            Map<String, Object> result = fileAnalysisService.analyzeAndSaveReport(file);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Upload failed", e);
            String message = e.getMessage() == null ? "Upload failed" : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", true,
                        "message", message,
                        "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    /**
     * Analyze an uploaded report (or ask a question about it)
     * Used by Copilot UI to get analysis
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzeReport(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "question", required = false) String question
    ) {
        try {
            log.info("Analyze request: files={}, question={}", files == null ? 0 : files.length, question);
            
            String answer = "";
            Integer score = 0;
            List<String> findings = new ArrayList<>();
            String extractedText = "";
            Long reportId = null;

            // Process uploaded files
            if (files != null && files.length > 0) {
                for (MultipartFile f : files) {
                    if (f == null || f.isEmpty()) continue;
                    try {
                        Map<String, Object> analysisResult = fileAnalysisService.analyzeAndSaveReport(f);
                        
                        // Extract analysis data from result
                        if (analysisResult != null) {
                            answer = (String) analysisResult.getOrDefault("answer", answer);
                            Integer s = (Integer) analysisResult.get("score");
                            if (s != null && s > score) score = s;
                            
                            @SuppressWarnings("unchecked")
                            List<String> findingsList = (List<String>) analysisResult.get("findings");
                            if (findingsList != null) findings.addAll(findingsList);
                            
                            String et = (String) analysisResult.get("extractedText");
                            if (et != null && extractedText.isEmpty()) extractedText = et;
                            
                            reportId = (Long) analysisResult.get("reportId");
                        }
                    } catch (Exception ex) {
                        log.error("Failed to process file: {}", f.getOriginalFilename(), ex);
                    }
                }
            }

            // Build response
            Map<String, Object> resp = new HashMap<>();
            resp.put("answer", answer.isEmpty() ? "Analysis completed successfully." : answer);
            resp.put("score", score);
            resp.put("findings", findings);
            resp.put("extractedText", extractedText);
            resp.put("reportId", reportId);

            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("Analysis error", e);
            String message = e.getMessage() == null ? "Analysis failed" : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", message));
        }
    }

    /**
     * Get all uploaded reports
     */
    @GetMapping("/reports")
    public ResponseEntity<?> getAllReports() {
        try {
            List<HealthReport> reports = reportRepository.findAll();
            List<Map<String, ? extends Serializable>> result = reports.stream()
                    .map(r -> Map.of(
                        "id", r.getId(),
                        "fileName", r.getUploadedFileName() != null ? r.getUploadedFileName() : "Unknown",
                        "patientName", r.getPatientName() != null ? r.getPatientName() : "N/A",
                        "score", r.getRiskScore() != null ? r.getRiskScore() : 0,
                        "createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "",
                        "glucose", r.getGlucose() != null ? r.getGlucose() : 0,
                        "cholesterol", r.getCholesterol() != null ? r.getCholesterol() : 0
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("reports", result));
        } catch (Exception e) {
            log.error("Failed to fetch reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", "Failed to fetch reports"));
        }
    }

    /**
     * Get a specific report by ID
     */
    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReportById(@PathVariable Long id) {
        try {
            return reportRepository.findById(id)
                    .map(report -> ResponseEntity.ok(Map.of(
                        "id", report.getId(),
                        "fileName", report.getUploadedFileName() != null ? report.getUploadedFileName() : "Unknown",
                        "patientName", report.getPatientName() != null ? report.getPatientName() : "N/A",
                        "age", report.getAge() != null ? report.getAge() : 0,
                        "glucose", report.getGlucose() != null ? report.getGlucose() : 0,
                        "cholesterol", report.getCholesterol() != null ? report.getCholesterol() : 0,
                        "score", report.getRiskScore() != null ? report.getRiskScore() : 0,
                        "suggestions", report.getSuggestions() != null ? report.getSuggestions() : "",
                        "extractedText", report.getExtractedText() != null ? report.getExtractedText() : "",
                        "createdAt", report.getCreatedAt() != null ? report.getCreatedAt().toString() : ""
                    )))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to fetch report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", "Failed to fetch report"));
        }
    }

    /**
     * Get the latest report
     */
    @GetMapping("/reports/latest")
    public ResponseEntity<?> getLatestReport() {
        try {
            return reportRepository.findTopByOrderByIdDesc()
                    .map(report -> ResponseEntity.ok(Map.of(
                        "id", report.getId(),
                        "fileName", report.getUploadedFileName() != null ? report.getUploadedFileName() : "Unknown",
                        "patientName", report.getPatientName() != null ? report.getPatientName() : "N/A",
                        "age", report.getAge() != null ? report.getAge() : 0,
                        "glucose", report.getGlucose() != null ? report.getGlucose() : 0,
                        "cholesterol", report.getCholesterol() != null ? report.getCholesterol() : 0,
                        "score", report.getRiskScore() != null ? report.getRiskScore() : 0,
                        "suggestions", report.getSuggestions() != null ? report.getSuggestions() : "",
                        "createdAt", report.getCreatedAt() != null ? report.getCreatedAt().toString() : ""
                    )))
                    .orElse(ResponseEntity.ok(Map.of("message", "No reports found")));
        } catch (Exception e) {
            log.error("Failed to fetch latest report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", true, "message", "Failed to fetch latest report"));
        }
    }
}