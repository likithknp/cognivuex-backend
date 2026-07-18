package com.cognivuex.controller;

import com.cognivuex.dto.ConversationMessage;
import com.cognivuex.entity.Conversation;
import com.cognivuex.entity.HealthReport;
import com.cognivuex.repository.HealthReportRepository;
import com.cognivuex.service.ConversationService;
import com.cognivuex.service.OpenAIService;
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
    private final OpenAIService openAIService;
    private final ConversationService conversationService;

    public ReportUploadController(
            FileAnalysisService fileAnalysisService,
            HealthReportRepository reportRepository,
            OpenAIService openAIService,
            ConversationService conversationService
    ) {
        this.fileAnalysisService = fileAnalysisService;
        this.reportRepository = reportRepository;
        this.openAIService = openAIService;
        this.conversationService = conversationService;
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
     * AI Copilot chat endpoint - powered by ChatGPT
     * Accepts question and optional files, returns AI response with context
     */
    @PostMapping(value = "/copilot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> copilotChat(
            @RequestParam(value = "question", required = true) String question,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) {
        try {
            log.info("Copilot request: question={}, files={}", question, files == null ? 0 : files.length);

            HealthReport report = null;
            String extractedText = "";
            Integer score = 0;
            List<String> findings = new ArrayList<>();
            Long reportId = null;

            // Process uploaded files if provided
            if (files != null && files.length > 0) {
                for (MultipartFile f : files) {
                    if (f == null || f.isEmpty()) continue;
                    try {
                        Map<String, Object> analysisResult = fileAnalysisService.analyzeAndSaveReport(f);
                        
                        if (analysisResult != null) {
                            Object reportIdObj = analysisResult.get("reportId");
                            if (reportIdObj != null && reportIdObj instanceof Long) {
                                reportId = (Long) reportIdObj;
                                report = reportRepository.findById(reportId).orElse(null);
                            }
                            
                            String et = (String) analysisResult.get("extractedText");
                            if (et != null && extractedText.isEmpty()) extractedText = et;
                            
                            Integer s = (Integer) analysisResult.get("score");
                            if (s != null && s > score) score = s;
                            
                            @SuppressWarnings("unchecked")
                            List<String> findingsList = (List<String>) analysisResult.get("findings");
                            if (findingsList != null) findings.addAll(findingsList);
                        }
                    } catch (Exception ex) {
                        log.error("Failed to process file: {}", f.getOriginalFilename(), ex);
                    }
                }
            }

            // If no files provided, use the latest report
            if (report == null) {
                report = reportRepository.findTopByOrderByIdDesc().orElse(null);
                if (report != null) {
                    reportId = report.getId();
                    extractedText = report.getExtractedText() != null ? report.getExtractedText() : "";
                    score = report.getRiskScore() != null ? report.getRiskScore() : 0;
                }
            }

            // If still no report, return error
            if (report == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "error", true,
                            "message", "No health report found. Please upload a report first.",
                            "answer", "I need a health report to analyze. Please upload one first.",
                            "score", 0,
                            "findings", new ArrayList<>(),
                            "extractedText", ""
                        ));
            }

            // Build conversation history for ChatGPT
            // Note: Since proper user auth isn't implemented, we use the report as context
            List<ConversationMessage> conversationHistory = new ArrayList<>();
            
            // TODO: When JWT auth is implemented, load user-specific conversation history:
            // Conversation conversation = conversationService.getOrCreateConversation(user, report);
            // conversationHistory = conversationService.getConversationHistory(conversation);

            // Call OpenAI with the question, report context, and conversation history
            String reportText = extractedText != null ? extractedText : "";
            String patientName = report.getPatientName() != null ? report.getPatientName() : "Patient";
            
            String aiResponse = openAIService.analyzeHealthReport(reportText, question, conversationHistory, patientName);

            // TODO: When JWT auth is implemented, save to conversation history:
            // conversationService.addExchangeToConversation(conversation, question, aiResponse);

            // Build response in the format expected by frontend
            Map<String, Object> resp = new HashMap<>();
            resp.put("answer", aiResponse);
            resp.put("score", score);
            resp.put("findings", findings);
            resp.put("extractedText", extractedText);
            resp.put("reportId", reportId);

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            log.error("Copilot error", e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "AI analysis failed";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", true,
                        "message", errorMessage,
                        "answer", "Sorry, I encountered an error while analyzing your report. Please try again.",
                        "score", 0,
                        "findings", new ArrayList<>(),
                        "extractedText", ""
                    ));
        }
    }

}
