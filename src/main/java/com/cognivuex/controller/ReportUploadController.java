package com.cognivuex.controller;

import com.cognivuex.dto.PredictionResponseDTO;
import com.cognivuex.service.FileAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public PredictionResponseDTO uploadReport(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        return fileAnalysisService.processReport(file);
    }
}