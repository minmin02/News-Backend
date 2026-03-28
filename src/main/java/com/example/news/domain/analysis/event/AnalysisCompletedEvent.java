package com.example.news.domain.analysis.event;

import java.util.List;

public record AnalysisCompletedEvent(
        Long targetId,
        Long analysisJobId,
        Long biasAnalysisResultId,
        String status,
        Double overallBiasScore,
        List<String> analysisKeywords
) {}
