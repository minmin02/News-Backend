package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SentenceLabelResultDto(
        Long contentSentenceId,
        Integer startOffset,
        Integer endOffset,
        String labelType,
        Double score,
        String matchedWord
) {}
