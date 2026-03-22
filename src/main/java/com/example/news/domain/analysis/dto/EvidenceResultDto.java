package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EvidenceResultDto(
        Long contentSentenceId,
        String evidenceType,
        String title,
        String description,
        String sourceText,
        Double confidenceScore
) {}
