package com.example.news.domain.analysis.event;

import java.util.List;
import java.util.Map;

public record AnalysisCompletedEvent(
        Long targetId,
        Long analysisJobId,
        Long biasAnalysisResultId,
        String status,
        Double overallBiasScore,
        List<String> analysisKeywords,
        String summaryText,
        Map<String, Double> biasTypeScores
) {}
