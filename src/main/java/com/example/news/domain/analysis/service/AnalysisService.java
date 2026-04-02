package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.AnalyzeRawTextRequestDto;
import com.example.news.domain.analysis.dto.BiasAnalysisResultDto;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.analysis.entity.BiasAnalysisKeyword;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.BiasEvidence;
import com.example.news.domain.analysis.entity.ContentSentence;
import com.example.news.domain.analysis.entity.HighlightResult;
import com.example.news.domain.analysis.entity.HighlightSpan;
import com.example.news.domain.analysis.entity.SentenceBiasLabel;
import com.example.news.domain.analysis.enums.BiasKeywordType;
import com.example.news.domain.analysis.enums.EvidenceType;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.SentenceLabelType;
import com.example.news.domain.analysis.enums.SentenceTargetType;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * transcript 원문 텍스트를 Python /analyze/raw로 전달하여 분석 작업을 생성하고 실행한다.
     * Python이 문장 분리를 수행하며, 응답의 sentences를 ContentSentence로 저장한다.
     *
     * @param transcriptId YoutubeTranscript DB PK
     * @param title        영상 제목 (headline-body gap 분석에 사용)
     * @param language     언어 코드 (ko, en 등)
     * @param rawText      transcript 원문 텍스트
     * @return 생성된 AnalysisJob
     */
    @Transactional
    public AnalysisJob createAnalysisJobFromRawText(YoutubeTranscript transcript) {

        Long transcriptId = transcript.getId();

        AnalysisJob job = AnalysisJob.builder()
                .targetId(transcriptId)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();

        final AnalysisJob savedJob = analysisJobRepository.save(job);

        AnalysisCompletedEvent completedEvent = null;

        try {
            // 1. RUNNING 전이
            savedJob.start();

            // 2. Python /analyze/raw 호출
            AnalyzeRawTextRequestDto request = new AnalyzeRawTextRequestDto(
                    transcriptId,
                    transcript.getYoutubeVideo().getTitle(),
                    transcript.getLanguageCode(),
                    transcript.getTranscriptText(),
                    "YOUTUBE_VIDEO",
                    transcriptId,
                    transcript.getYoutubeVideo().getCountryCode());

            BiasAnalysisResultDto result = webClient.post()
                    .uri(pythonBaseUrl + "/analyze/raw")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BiasAnalysisResultDto.class)
                    .block();

            // 3. ContentSentence 저장 + pythonId(sentenceOrder) → DB ID 매핑 생성
            Map<Long, Long> pythonIdToDbId = new HashMap<>();
            if (result.sentences() != null && !result.sentences().isEmpty()) {
                List<ContentSentence> savedSentences = contentSentenceRepository.saveAll(
                        result.sentences().stream()
                                .map(s -> ContentSentence.builder()
                                        .targetId(transcriptId)
                                        .targetType(SentenceTargetType.YOUTUBE_TRANSCRIPT)
                                        .sentenceOrder(s.sentenceOrder())
                                        .sentenceText(s.sentenceText())
                                        .startTimeMs(s.startTimeMs())
                                        .endTimeMs(s.endTimeMs())
                                        .build())
                                .toList()
                );
                pythonIdToDbId = savedSentences.stream()
                        .collect(Collectors.toMap(
                                s -> s.getSentenceOrder().longValue(),
                                ContentSentence::getId
                        ));
            }

            // 4. BiasAnalysisResult 저장
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

            // 5. BiasAnalysisKeyword 저장
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

            // 6. SentenceBiasLabel 저장 (pythonId → DB ID 매핑 적용)
            final Map<Long, Long> idMap = pythonIdToDbId;
            if (result.sentenceLabels() != null) {
                sentenceBiasLabelRepository.saveAll(
                        result.sentenceLabels().stream()
                                .filter(l -> idMap.containsKey(l.contentSentenceId()))
                                .map(l -> SentenceBiasLabel.builder()
                                        .analysisJob(savedJob)
                                        .contentSentence(contentSentenceRepository.getReferenceById(idMap.get(l.contentSentenceId())))
                                        .labelType(SentenceLabelType.valueOf(l.labelType().toUpperCase()))
                                        .score(l.score())
                                        .highlightColor(l.highlightColor())
                                        .evidenceKeyword(l.evidenceKeyword())
                                        .build())
                                .toList()
                );
            }

            // 7. BiasEvidence 저장 (pythonId → DB ID 매핑 적용)
            if (result.evidences() != null) {
                biasEvidenceRepository.saveAll(
                        result.evidences().stream()
                                .filter(e -> idMap.containsKey(e.contentSentenceId()))
                                .map(e -> BiasEvidence.builder()
                                        .biasAnalysisResult(savedResult)
                                        .contentSentence(contentSentenceRepository.getReferenceById(idMap.get(e.contentSentenceId())))
                                        .evidenceType(EvidenceType.valueOf(e.evidenceType().toUpperCase()))
                                        .title(e.title())
                                        .description(e.description())
                                        .sourceText(e.sourceText())
                                        .confidenceScore(e.confidenceScore())
                                        .build())
                                .toList()
                );
            }

            // 8. HighlightResult / HighlightSpan 저장
            if (result.sentenceLabels() != null && !result.sentenceLabels().isEmpty()) {
                HighlightResult highlightResult = highlightResultRepository.save(
                        HighlightResult.builder()
                                .biasAnalysisResult(savedResult)
                                .build()
                );
                highlightSpanRepository.saveAll(
                        result.sentenceLabels().stream()
                                .filter(l -> l.startOffset() != null && l.endOffset() != null)
                                .filter(l -> idMap.containsKey(l.contentSentenceId()))
                                .map(l -> HighlightSpan.builder()
                                        .highlightResult(highlightResult)
                                        .contentSentence(contentSentenceRepository.getReferenceById(idMap.get(l.contentSentenceId())))
                                        .startOffset(l.startOffset())
                                        .endOffset(l.endOffset())
                                        .labelType(SentenceLabelType.valueOf(l.labelType().toUpperCase()))
                                        .score(l.score())
                                        .matchedWord(l.matchedWord())
                                        .build())
                                .toList()
                );
            }

            // 9. SUCCESS 전이
            savedJob.complete();

            // 10. 이벤트 페이로드 준비
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
            log.error("Raw text analysis failed for job {}: {}", savedJob.getId(), e.getMessage());
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

}
