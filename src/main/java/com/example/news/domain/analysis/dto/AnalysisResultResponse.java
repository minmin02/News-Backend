package com.example.news.domain.analysis.dto;

import com.example.news.domain.analysis.enums.BiasKeywordType;
import com.example.news.domain.analysis.enums.EvidenceType;
import com.example.news.domain.analysis.enums.SentenceLabelType;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AnalysisResultResponse(
        Long targetId,
        Double overallBiasScore,
        Double opinionScore,
        Double emotionScore,
        Double anonymousSourceScore,
        Double headlineBodyGapScore,
        Double neutralityScore,
        String summaryText,
        String perspectiveSummary,
        String evidenceSummary,
        String toneLabel,
        List<KeywordItem> keywords,
        List<SentenceLabelItem> sentenceLabels,
        List<EvidenceItem> evidences,
        List<HighlightSpanItem> highlightSpans
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record KeywordItem(
            String keywordText,
            BiasKeywordType keywordType,
            Double score
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SentenceLabelItem(
            Long contentSentenceId,
            SentenceLabelType labelType,
            Double score,
            String highlightColor,
            String evidenceKeyword
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EvidenceItem(
            Long contentSentenceId,
            EvidenceType evidenceType,
            String title,
            String description,
            String sourceText,
            Double confidenceScore
    ) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record HighlightSpanItem(
            Long contentSentenceId,
            Integer startOffset,
            Integer endOffset,
            SentenceLabelType labelType,
            Double score,
            String matchedWord
    ) {}
}
