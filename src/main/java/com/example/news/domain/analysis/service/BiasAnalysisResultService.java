package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.AnalysisResultResponse;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.HighlightResult;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.exception.AnalysisException;
import com.example.news.domain.analysis.exception.code.AnalysisErrorCode;
import com.example.news.domain.analysis.repository.BiasAnalysisKeywordRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.analysis.repository.BiasEvidenceRepository;
import com.example.news.domain.analysis.repository.HighlightResultRepository;
import com.example.news.domain.analysis.repository.HighlightSpanRepository;
import com.example.news.domain.analysis.repository.SentenceBiasLabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BiasAnalysisResultService {

    private final BiasAnalysisResultRepository biasAnalysisResultRepository;
    private final BiasAnalysisKeywordRepository biasAnalysisKeywordRepository;
    private final BiasEvidenceRepository biasEvidenceRepository;
    private final SentenceBiasLabelRepository sentenceBiasLabelRepository;
    private final HighlightResultRepository highlightResultRepository;
    private final HighlightSpanRepository highlightSpanRepository;

    @Transactional(readOnly = true)
    public AnalysisResultResponse getAnalysisResult(Long targetId) {
        BiasAnalysisResult result = biasAnalysisResultRepository
                .findByTargetIdAndTargetType(targetId, TargetType.YOUTUBE_VIDEO)
                .orElseThrow(() -> new AnalysisException(AnalysisErrorCode.ANALYSIS_RESULT_NOT_FOUND));

        var keywords = biasAnalysisKeywordRepository.findAllByBiasAnalysisResultId(result.getId())
                .stream()
                .map(k -> new AnalysisResultResponse.KeywordItem(
                        k.getKeywordText(),
                        k.getKeywordType(),
                        k.getScore()
                ))
                .toList();

        var evidences = biasEvidenceRepository.findAllByBiasAnalysisResultId(result.getId())
                .stream()
                .map(e -> new AnalysisResultResponse.EvidenceItem(
                        e.getContentSentence().getId(),
                        e.getEvidenceType(),
                        e.getTitle(),
                        e.getDescription(),
                        e.getSourceText(),
                        e.getConfidenceScore()
                ))
                .toList();

        // SentenceBiasLabel은 AnalysisJob을 통해 조회
        // analysisJob이 null인 경우 빈 리스트 반환
        var sentenceLabels = Optional.ofNullable(result.getAnalysisJob())
                .map(job -> sentenceBiasLabelRepository.findAllByAnalysisJobId(job.getId())
                        .stream()
                        .map(l -> new AnalysisResultResponse.SentenceLabelItem(
                                l.getContentSentence().getId(),
                                l.getLabelType(),
                                l.getScore(),
                                l.getHighlightColor(),
                                l.getEvidenceKeyword()
                        ))
                        .toList())
                .orElse(List.of());

        // HighlightResult가 아직 저장되지 않은 경우 빈 리스트 반환
        var highlightSpans = highlightResultRepository.findByBiasAnalysisResultId(result.getId())
                .map(HighlightResult::getId)
                .map(highlightSpanRepository::findByHighlightResultId)
                .orElse(List.of())
                .stream()
                .map(s -> new AnalysisResultResponse.HighlightSpanItem(
                        s.getContentSentence().getId(),
                        s.getStartOffset(),
                        s.getEndOffset(),
                        s.getLabelType(),
                        s.getScore(),
                        s.getMatchedWord()
                ))
                .toList();

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
