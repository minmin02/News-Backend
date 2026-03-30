package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.AnalyzeRequestDto;
import com.example.news.domain.analysis.dto.BiasAnalysisResultDto;
import com.example.news.domain.analysis.dto.ContentPreparedEventDto;
import com.example.news.domain.analysis.dto.SentenceInputDto;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.entity.BiasAnalysisKeyword;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.BiasEvidence;
import com.example.news.domain.analysis.entity.HighlightResult;
import com.example.news.domain.analysis.entity.HighlightSpan;
import com.example.news.domain.analysis.entity.SentenceBiasLabel;
import com.example.news.domain.analysis.enums.BiasKeywordType;
import com.example.news.domain.analysis.enums.EvidenceType;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.SentenceLabelType;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.AnalysisJobRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisKeywordRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.analysis.repository.BiasEvidenceRepository;
import com.example.news.domain.analysis.repository.ContentSentenceRepository;
import com.example.news.domain.analysis.event.AnalysisCompletedEvent;
import com.example.news.domain.analysis.repository.HighlightResultRepository;
import com.example.news.domain.analysis.repository.HighlightSpanRepository;
import com.example.news.domain.analysis.repository.SentenceBiasLabelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisJobRepository analysisJobRepository;
    private final ContentSentenceRepository contentSentenceRepository;
    private final BiasAnalysisResultRepository biasAnalysisResultRepository;
    private final BiasAnalysisKeywordRepository biasAnalysisKeywordRepository;
    private final SentenceBiasLabelRepository sentenceBiasLabelRepository;
    private final BiasEvidenceRepository biasEvidenceRepository;
    private final HighlightResultRepository highlightResultRepository;
    private final HighlightSpanRepository highlightSpanRepository;
    private final WebClient webClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${python.base-url}")
    private String pythonBaseUrl;

    /**
     * Content BC로부터 받은 이벤트를 기반으로 분석 작업을 생성하고 실행한다.
     *
     * 1. AnalysisJob을 PENDING 상태로 DB에 저장
     * 2. Python FastAPI POST /analyze 호출하여 편향 분석 실행
     * 3. 분석 결과를 BiasAnalysisResult로 저장
     * 4. 실패 시 AnalysisJob 상태를 FAILED로 전이하고 로그만 남긴다
     *
     * @param event Content BC에서 전달받은 영상 및 문장 정보
     * @return 생성된 AnalysisJob
     */
    @Transactional
    public AnalysisJob createAnalysisJob(ContentPreparedEventDto event) {
        AnalysisJob job = AnalysisJob.builder()
                .targetId(event.youtubeTranscriptId())
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        final AnalysisJob savedJob = analysisJobRepository.save(job);

        AnalysisCompletedEvent completedEvent = null;

        try {
            // 1. RUNNING 전이
            savedJob.start();

            // 2. Python FastAPI 호출
            List<SentenceInputDto> sentences = getSentenceInputs(event.youtubeTranscriptId());
            AnalyzeRequestDto request = new AnalyzeRequestDto(event.youtubeTranscriptId(), event.title(), event.language(), sentences);
            BiasAnalysisResultDto result = webClient.post()
                    .uri(pythonBaseUrl + "/analyze")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BiasAnalysisResultDto.class)
                    .block();

            // 3. BiasAnalysisResult 저장
            BiasAnalysisResult savedResult = biasAnalysisResultRepository.save(
                    BiasAnalysisResult.builder()
                            .analysisJob(savedJob)
                            .targetId(savedJob.getTargetId())
                            .targetType(savedJob.getTargetType())
                            .overallBiasScore(result.overallBiasScore())
                            .opinionScore(result.opinionScore())
                            .emotionScore(result.emotionScore())
                            .anonymousSourceScore(result.anonymousSourceScore())
                            .headlineBodyGapScore(result.headlineBodyGapScore())
                            .neutralityScore(result.neutralityScore())
                            .summaryText(result.summaryText())
                            .perspectiveSummary(result.perspectiveSummary())
                            .evidenceSummary(result.evidenceSummary())
                            .toneLabel(result.toneLabel())
                            .build()
            );

            // 4. BiasAnalysisKeyword 리스트 저장
            if (result.keywords() != null) {
                biasAnalysisKeywordRepository.saveAll(
                        result.keywords().stream()
                                .map(k -> BiasAnalysisKeyword.builder()
                                        .biasAnalysisResult(savedResult)
                                        .keywordText(k.keywordText())
                                        .keywordType(BiasKeywordType.valueOf(k.keywordType().toUpperCase()))
                                        .score(k.score())
                                        .build())
                                .toList()
                );
            }

            // 5. SentenceBiasLabel 리스트 저장
            if (result.sentenceLabels() != null) {
                sentenceBiasLabelRepository.saveAll(
                        result.sentenceLabels().stream()
                                .map(l -> SentenceBiasLabel.builder()
                                        .analysisJob(savedJob)
                                        .contentSentence(contentSentenceRepository.getReferenceById(l.contentSentenceId()))
                                        .labelType(SentenceLabelType.valueOf(l.labelType().toUpperCase()))
                                        .score(l.score())
                                        .highlightColor(l.highlightColor())
                                        .evidenceKeyword(l.evidenceKeyword())
                                        .build())
                                .toList()
                );
            }

            // 6. BiasEvidence 리스트 저장
            if (result.evidences() != null) {
                biasEvidenceRepository.saveAll(
                        result.evidences().stream()
                                .map(e -> BiasEvidence.builder()
                                        .biasAnalysisResult(savedResult)
                                        .contentSentence(contentSentenceRepository.getReferenceById(e.contentSentenceId()))
                                        .evidenceType(EvidenceType.valueOf(e.evidenceType().toUpperCase()))
                                        .title(e.title())
                                        .description(e.description())
                                        .sourceText(e.sourceText())
                                        .confidenceScore(e.confidenceScore())
                                        .build())
                                .toList()
                );
            }

            // 7. HighlightResult / HighlightSpan 저장
            // offset 정보(startOffset, endOffset)가 있는 label만 span으로 저장
            if (result.sentenceLabels() != null && !result.sentenceLabels().isEmpty()) {
                HighlightResult highlightResult = highlightResultRepository.save(
                        HighlightResult.builder()
                                .biasAnalysisResult(savedResult)
                                .build()
                );
                highlightSpanRepository.saveAll(
                        result.sentenceLabels().stream()
                                .filter(l -> l.startOffset() != null && l.endOffset() != null)
                                .map(l -> HighlightSpan.builder()
                                        .highlightResult(highlightResult)
                                        .contentSentence(contentSentenceRepository.getReferenceById(l.contentSentenceId()))
                                        .startOffset(l.startOffset())
                                        .endOffset(l.endOffset())
                                        .labelType(SentenceLabelType.valueOf(l.labelType().toUpperCase()))
                                        .score(l.score())
                                        .matchedWord(l.matchedWord())
                                        .build())
                                .toList()
                );
            }

            // 8. SUCCESS 전이
            savedJob.complete();

            // 9. 이벤트 페이로드 준비 (발행은 try 밖에서 — 발행 실패가 분석 실패로 이어지지 않도록)
            List<String> analysisKeywords = result.keywords() != null
                    ? result.keywords().stream().map(k -> k.keywordText()).toList()
                    : List.of();
            completedEvent = new AnalysisCompletedEvent(
                    savedJob.getTargetId(),
                    savedJob.getId(),
                    savedResult.getId(),
                    JobStatus.SUCCESS.name(),
                    result.overallBiasScore(),
                    analysisKeywords,
                    result.summaryText(),
                    result.biasTypeScores()
            );

        } catch (Exception e) {
            log.error("Analysis failed for job {}: {}", savedJob.getId(), e.getMessage());
            savedJob.fail(e.getMessage());
        }

        if (completedEvent != null) {
            try {
                eventPublisher.publishEvent(completedEvent);
            } catch (Exception e) {
                log.error("AnalysisCompletedEvent 발행 실패 (jobId={})", completedEvent.analysisJobId(), e);
            }
        }

        return savedJob;
    }

    /**
     * 특정 transcript에 속한 문장 목록을 순서대로 조회하여 분석 요청 단위로 변환한다.
     *
     * Python FastAPI 호출 전 sentence 입력값을 준비하는 용도로 사용된다.
     * sentence_order 기준 오름차순 정렬된 상태로 반환된다.
     *
     * @param transcriptId 조회할 youtube_transcript의 id
     * @return sentenceOrder 기준 정렬된 SentenceInputDto 리스트
     */
    @Transactional(readOnly = true)
    public List<SentenceInputDto> getSentenceInputs(Long transcriptId) {
        return contentSentenceRepository
                .findAllByTargetIdOrderBySentenceOrder(transcriptId)
                .stream()
                .map(s -> new SentenceInputDto(
                        s.getId(),
                        s.getSentenceText(),
                        s.getSentenceOrder(),
                        s.getStartTimeMs(),
                        s.getEndTimeMs()
                ))
                .toList();
    }
}
