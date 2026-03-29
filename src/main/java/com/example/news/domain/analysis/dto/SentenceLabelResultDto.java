package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.lang.Nullable;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SentenceLabelResultDto(
        Long contentSentenceId,
        String labelType,
        Double score,
        @Nullable String highlightColor,
        @Nullable String evidenceKeyword,
        // Python SpanLabelDto에서 오는 span 위치 정보 (HighlightSpan 저장에 사용)
        @Nullable Integer startOffset,
        @Nullable Integer endOffset,
        @Nullable String matchedWord
) {}
