package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BiasAnalysisResultResponse(
        Long targetId,
        String targetType,
        Long transcriptId,

        Double overallBiasScore,
        Double opinionScore,
        Double emotionScore,
        @Nullable Double headlineBodyGapScore,

        String summaryText,
        String perspectiveSummary,
        String evidenceSummary,
        String toneLabel,

        Double factRatio,
        String scoreEvidence,
        Map<String, Double> biasTypeScores,

        @Nullable List<KeywordResultDto> keywords,
        @Nullable List<SentenceLabelResultDto> sentenceLabels,
        @Nullable List<EvidenceResultDto> evidences,
        @Nullable List<SentenceResultResponse> sentences
) {}
