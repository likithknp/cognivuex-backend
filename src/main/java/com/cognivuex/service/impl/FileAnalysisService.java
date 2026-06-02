package com.cognivuex.service;

import com.cognivuex.dto.PredictionResponseDTO;
import com.cognivuex.entity.HealthReport;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileAnalysisService {

    private final PredictionService predictionService;

    public FileAnalysisService(
            PredictionService predictionService
    ) {
        this.predictionService = predictionService;
    }

    public PredictionResponseDTO processReport(
            MultipartFile multipartFile
    ) throws Exception {

        // Create temp file
        File file = File.createTempFile(
                "report",
                multipartFile.getOriginalFilename()
        );

        multipartFile.transferTo(file);

        // OCR Engine
        Tesseract tesseract = new Tesseract();

        // tessdata folder path
        tesseract.setDatapath("D:\\Sneha project\\tessaract\\tessdata");

        // Extract text
        String extractedText = tesseract.doOCR(file);

        System.out.println("===== OCR TEXT =====");
        System.out.println(extractedText);

        // Create report object
        HealthReport report = new HealthReport();

        report.setUploadedFileName(
                multipartFile.getOriginalFilename()
        );

        report.setExtractedText(extractedText);

        report.setPatientName(
                extractText(extractedText, "Name")
        );

        report.setAge(
                extractInteger(extractedText, "Age")
        );

        report.setGlucose(
                extractInteger(extractedText, "Glucose")
        );

        report.setCholesterol(
                extractInteger(extractedText, "Cholesterol")
        );

        report.setBmi(
                extractDouble(extractedText, "BMI")
        );

        report.setHeartRate(
                extractInteger(extractedText, "Heart Rate")
        );

        report.setHba1c(
                extractDouble(extractedText, "HbA1c")
        );

        // Blood Pressure Extraction
        Integer[] bp = extractBloodPressure(extractedText);

        report.setSystolicBP(bp[0]);
        report.setDiastolicBP(bp[1]);

        // Analyze prediction
        return predictionService.analyze(report);
    }

    // Extract Integer Values
    private Integer extractInteger(
            String text,
            String keyword
    ) {

        Pattern pattern = Pattern.compile(
                keyword + "\\s*[:\\-]?\\s*(\\d+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Integer.parseInt(
                    matcher.group(1)
            );
        }

        return 0;
    }

    // Extract Decimal Values
    private Double extractDouble(
            String text,
            String keyword
    ) {

        Pattern pattern = Pattern.compile(
                keyword + "\\s*[:\\-]?\\s*(\\d+(\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return Double.parseDouble(
                    matcher.group(1)
            );
        }

        return 0.0;
    }

    // Extract Name
    private String extractText(
            String text,
            String keyword
    ) {

        Pattern pattern = Pattern.compile(
                keyword + "\\s*[:\\-]?\\s*([A-Za-z ]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "Unknown";
    }

    // Extract BP like 120/80
    private Integer[] extractBloodPressure(
            String text
    ) {

        Pattern pattern = Pattern.compile(
                "(\\d{2,3})\\s*/\\s*(\\d{2,3})"
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            Integer systolic =
                    Integer.parseInt(
                            matcher.group(1)
                    );

            Integer diastolic =
                    Integer.parseInt(
                            matcher.group(2)
                    );

            return new Integer[]{
                    systolic,
                    diastolic
            };
        }

        return new Integer[]{0, 0};
    }
}