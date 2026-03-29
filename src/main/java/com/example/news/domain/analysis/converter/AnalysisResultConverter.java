package com.example.news.domain.analysis.converter;

import com.example.news.domain.analysis.dto.AnalysisResultResponse;
import com.example.news.domain.analysis.entity.BiasAnalysisKeyword;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.BiasEvidence;
import com.example.news.domain.analysis.entity.HighlightSpan;
import com.example.news.domain.analysis.entity.SentenceBiasLabel;

import java.util.List;

public class AnalysisResultConverter {

    public static AnalysisResultResponse.KeywordItem toKeywordItem(BiasAnalysisKeyword keyword) {
        return new AnalysisResultResponse.KeywordItem(
                keyword.getKeywordText(),
                keyword.getKeywordType(),
                keyword.getScore()
        );
    }

    public static AnalysisResultResponse.EvidenceItem toEvidenceItem(BiasEvidence evidence) {
        return new AnalysisResultResponse.EvidenceItem(
                evidence.getContentSentence().getId(),
                evidence.getEvidenceType(),
                evidence.getTitle(),
                evidence.getDescription(),
                evidence.getSourceText(),
                evidence.getConfidenceScore()
        );
    }

    public static AnalysisResultResponse.SentenceLabelItem toSentenceLabelItem(SentenceBiasLabel label) {
        return new AnalysisResultResponse.SentenceLabelItem(
                label.getContentSentence().getId(),
                label.getLabelType(),
                label.getScore(),
                label.getHighlightColor(),
                label.getEvidenceKeyword()
        );
    }

    public static AnalysisResultResponse.HighlightSpanItem toHighlightSpanItem(HighlightSpan span) {
        return new AnalysisResultResponse.HighlightSpanItem(
                span.getContentSentence().getId(),
                span.getStartOffset(),
                span.getEndOffset(),
                span.getLabelType(),
                span.getScore(),
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
                result.getAnonymousSourceScore(),
                result.getHeadlineBodyGapScore(),
                result.getNeutralityScore(),
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
