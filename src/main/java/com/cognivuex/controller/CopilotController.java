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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class CopilotController {

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
            List<UploadResponseDTO> processed = new ArrayList<>();

            if (files != null) {
                for (MultipartFile f : files) {
                    if (f == null || f.isEmpty()) continue;
                    UploadResponseDTO dto = fileAnalysisService.processReport(f);
                    processed.add(dto);
                }
            }

            // Build response using the first processed report (if any)
            String answer = "";
            Integer score = null;
            List<String> findings = new ArrayList<>();
            String extractedText = "";

            if (!processed.isEmpty()) {
                UploadResponseDTO first = processed.get(0);
                if (first.getPrediction() != null) {
                    score = first.getPrediction().getRiskScore();
                    if (first.getPrediction().getAiRecommendation() != null && !first.getPrediction().getAiRecommendation().isBlank()) {
                        answer = first.getPrediction().getAiRecommendation();
                    }
                }
                HealthReport r = first.getReport();
                if (r != null) {
                    if ((answer == null || answer.isBlank()) && r.getSuggestions() != null) answer = r.getSuggestions();
                    if (r.getDiseaseRisks() != null) {
                        findings = Arrays.stream(r.getDiseaseRisks().split("[;\\n]"))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                    }
                    String et = r.getExtractedText() == null ? "" : r.getExtractedText();
                    extractedText = et.length() > 2000 ? et.substring(0, 2000) : et;
                }
            }

            // If a follow-up question is provided, try to ask the MedicalAIService using the combined extracted text
            if (question != null && !question.isBlank()) {
                String combinedText = processed.stream()
                        .map(p -> p.getReport() == null ? "" : p.getReport().getExtractedText())
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("\n\n"));

                try {
                    String aiOut = medicalAIService.analyze(combinedText + "\n\nQuestion: " + question);
                    if (aiOut != null && !aiOut.isBlank()) {
                        answer = aiOut;
                    }
                } catch (Exception ex) {
                    // If AI call fails (no key configured), fall back to existing answer
                }
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("answer", answer == null ? "" : answer);
            resp.put("score", score == null ? 0 : score);
            resp.put("findings", findings);
            resp.put("extractedText", extractedText);
            resp.put("reports", processed.stream().map(UploadResponseDTO::getReport).collect(Collectors.toList()));

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "copilot processing failed" : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", message));
        }
    }
}
