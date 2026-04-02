package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BiasAnalysisResultDto(
        Long targetId,
        Double overallBiasScore,
        Double opinionScore,
        Double emotionScore,
        Double anonymousSourceScore,
        @Nullable Double headlineBodyGapScore,
        @Nullable Double neutralityScore,
        String summaryText,
        String perspectiveSummary,
        String evidenceSummary,
        String toneLabel,
        Double subjectivityScore,
        String scoreEvidence,
        Map<String, Double> biasTypeScores,
        @Nullable List<SentenceInputDto> sentences,
        @Nullable List<KeywordResultDto> keywords,
        @Nullable List<SentenceLabelResultDto> sentenceLabels,
        @Nullable List<EvidenceResultDto> evidences
) {}