package com.example.news.domain.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisCompletedEventDto {

    private Long targetId;
    private Long analysisJobId;
    private Long biasAnalysisResultId;
    private String status;
    private Double overallBiasScore;
    private List<String> analysisKeywords;
    private String summaryText;
    private Map<String, Double> biasTypeScores;
}