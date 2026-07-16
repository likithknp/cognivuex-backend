package com.cognivuex.controller;

import com.cognivuex.dto.UploadResponseDTO;
import com.cognivuex.entity.HealthReport;
import com.cognivuex.service.MedicalAIService;
import com.cognivuex.service.impl.FileAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class CopilotController {

    private static final Logger log = LoggerFactory.getLogger(CopilotController.class);
    private final FileAnalysisService fileAnalysisService;
    private final MedicalAIService medicalAIService;

    public CopilotController(
            FileAnalysisService fileAnalysisService,
            MedicalAIService medicalAIService
    ) {
        this.fileAnalysisService = fileAnalysisService;
        this.medicalAIService = medicalAIService;
    }

    @PostMapping(value = "/copilot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> copilot(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "question", required = false) String question
    ) {
        try {
            log.info("Copilot request: files={}, question={}", files == null ? 0 : files.length, question);
            
            String answer = "";
            Integer score = 0;
            List<String> findings = new ArrayList<>();
            String extractedText = "";

            // Process uploaded files
            if (files != null && files.length > 0) {
                for (MultipartFile f : files) {
                    if (f == null || f.isEmpty()) continue;
                    try {
                        UploadResponseDTO dto = fileAnalysisService.processReport(f);
                        if (dto != null && dto.report != null) {
                            HealthReport r = dto.report;
                            if (answer.isEmpty() && r.suggestions != null) {
                                answer = r.suggestions;
                            }
                            if (score == 0 && r.riskScore != null) {
                                score = r.riskScore;
                            }
                            if (r.diseaseRisks != null && !r.diseaseRisks.isBlank()) {
                                String[] riskItems = r.diseaseRisks.split("[;\\n]");
                                for (String item : riskItems) {
                                    String trimmed = item.trim();
                                    if (!trimmed.isEmpty()) findings.add(trimmed);
                                }
                            }
                            if (extractedText.isEmpty() && r.extractedText != null) {
                                extractedText = r.extractedText.length() > 2000 
                                    ? r.extractedText.substring(0, 2000)
                                    : r.extractedText;
                            }
                        }
                    } catch (Exception ex) {
                        log.error("Failed to process file: {}", f.getOriginalFilename(), ex);
                    }
                }
            }

            // If a question is asked and we have extracted text, try AI analysis
            if (question != null && !question.isBlank() && !extractedText.isEmpty()) {
                try {
                    String aiPrompt = extractedText + "\n\nQuestion: " + question;
                    String aiOut = medicalAIService.analyze(aiPrompt);
                    if (aiOut != null && !aiOut.isBlank()) {
                        answer = aiOut;
                    }
                } catch (Exception ex) {
                    log.debug("AI analysis failed (may be missing API key): {}", ex.getMessage());
                }
            }

            // Build and return response
            Map<String, Object> resp = new HashMap<>();
            resp.put("answer", answer.isEmpty() ? "Analysis completed. No detailed recommendation available." : answer);
            resp.put("score", score == null ? 0 : score);
            resp.put("findings", findings);
            resp.put("extractedText", extractedText);

            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("Copilot error", e);
            String message = e.getMessage() == null ? "copilot processing failed" : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", message, "error", "true"));
        }
    }
}
