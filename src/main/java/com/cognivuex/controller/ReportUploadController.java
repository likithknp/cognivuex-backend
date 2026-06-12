package com.cognivuex.controller;

import com.cognivuex.dto.UploadResponseDTO;
import com.cognivuex.service.impl.FileAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class ReportUploadController {

    private final FileAnalysisService fileAnalysisService;

    public ReportUploadController(
            FileAnalysisService fileAnalysisService
    ) {
        this.fileAnalysisService = fileAnalysisService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadReport(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            UploadResponseDTO result = fileAnalysisService.processReport(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Log and return a helpful error body
            String message = e.getMessage() == null ? "Upload failed" : e.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", message));
        }
    }
}