package com.example.news.domain.analysis.converter;

import com.example.news.domain.analysis.dto.AnalysisResultResponse;
import com.example.news.domain.analysis.entity.BiasAnalysisKeyword;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.BiasEvidence;
import com.example.news.domain.analysis.entity.HighlightSpan;
import com.example.news.domain.analysis.entity.SentenceBiasLabel;

import java.util.List;

public class AnalysisResultConverter {

    private static Double clampScore(Double score) {
        if (score == null) return null;
        return Math.max(0.0, Math.min(1.0, score));
    }

    public static AnalysisResultResponse.KeywordItem toKeywordItem(BiasAnalysisKeyword keyword) {
        return new AnalysisResultResponse.KeywordItem(
                keyword.getKeywordText(),
                keyword.getKeywordType(),
                clampScore(keyword.getScore())
        );
    }

    public static AnalysisResultResponse.EvidenceItem toEvidenceItem(BiasEvidence evidence) {
        Long contentSentenceId = evidence.getContentSentence() != null
                ? evidence.getContentSentence().getId()
                : null;
        return new AnalysisResultResponse.EvidenceItem(
                contentSentenceId,
                evidence.getEvidenceType(),
                evidence.getTitle(),
                evidence.getDescription(),
                evidence.getSourceText(),
                clampScore(evidence.getConfidenceScore())
        );
    }

    public static AnalysisResultResponse.SentenceLabelItem toSentenceLabelItem(SentenceBiasLabel label) {
        return new AnalysisResultResponse.SentenceLabelItem(
                label.getContentSentence().getId(),
                label.getLabelType(),
                clampScore(label.getScore())
        );
    }

    public static AnalysisResultResponse.HighlightSpanItem toHighlightSpanItem(HighlightSpan span) {
        return new AnalysisResultResponse.HighlightSpanItem(
                span.getContentSentence().getId(),
                span.getStartOffset(),
                span.getEndOffset(),
                span.getLabelType(),
                clampScore(span.getScore()),
                span.getMatchedWord()
        );
    }

    public static AnalysisResultResponse toResponse(
            BiasAnalysisResult result,
            List<AnalysisResultResponse.KeywordItem> keywords,
            List<AnalysisResultResponse.SentenceLabelItem> sentenceLabels,
            List<AnalysisResultResponse.EvidenceItem> evidences,
            List<AnalysisResultResponse.HighlightSpanItem> highlightSpans
    ) {
        return new AnalysisResultResponse(
                result.getTargetId(),
                result.getOverallBiasScore(),
                result.getOpinionScore(),
                result.getEmotionScore(),
                result.getHeadlineBodyGapScore(),
                result.getSummaryText(),
                result.getPerspectiveSummary(),
                result.getEvidenceSummary(),
                result.getToneLabel(),
                keywords,
                sentenceLabels,
                evidences,
                highlightSpans
        );
    }
}
