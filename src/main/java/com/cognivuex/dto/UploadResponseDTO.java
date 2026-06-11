package com.cognivuex.dto;

import com.cognivuex.entity.HealthReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponseDTO {

    private HealthReport report;

    private PredictionResponseDTO prediction;

}

