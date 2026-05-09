package com.weanalyzer.dto;

import lombok.Data;

import java.util.List;

@Data
public class DiagnosisResponse {

    private String summary;
    private List<String> suggestions;
    private String trend;
    private List<String> riskPoints;
}
