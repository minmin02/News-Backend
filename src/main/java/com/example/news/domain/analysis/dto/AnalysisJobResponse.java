package com.example.news.domain.analysis.dto;

import com.example.news.domain.analysis.enums.JobStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AnalysisJobResponse(
        Long jobId,
        Long targetId,
        String targetType,
        Long transcriptId,
        JobStatus status
) {}
