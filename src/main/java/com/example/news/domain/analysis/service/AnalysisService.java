package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.BiasAnalysisResultDto;
import com.example.news.domain.analysis.dto.ContentPreparedEventDto;
import com.example.news.domain.analysis.dto.SentenceInputDto;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.exception.AnalysisException;
import com.example.news.domain.analysis.exception.code.AnalysisErrorCode;
import com.example.news.domain.analysis.repository.AnalysisJobRepository;
import com.example.news.domain.analysis.repository.ContentSentenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisJobRepository analysisJobRepository;
    private final ContentSentenceRepository contentSentenceRepository;

    /**
     * Content BC로부터 받은 이벤트를 기반으로 분석 작업을 생성하고 실행한다.
     *
     * 1. AnalysisJob을 PENDING 상태로 DB에 저장
     * 2. Python FastAPI POST /analyze 호출하여 편향 분석 실행 (TODO - B 담당)
     * 3. 분석 결과를 BiasAnalysisResult로 저장 (TODO - B 담당)
     * 4. 실패 시 AnalysisJob 상태를 FAILED로 전이하고 예외를 던진다
     *
     * @param event Content BC에서 전달받은 영상 및 문장 정보
     * @return 생성된 AnalysisJob
     * @throws AnalysisException Python 분석 호출 실패 또는 저장 실패 시
     */
    @Transactional
    public AnalysisJob createAnalysisJob(ContentPreparedEventDto event) {
        AnalysisJob job = AnalysisJob.builder()
                .targetId(event.youtubeTranscriptId())
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        job = analysisJobRepository.save(job);

        try {
            // TODO(B): POST /analyze 호출 후 BiasAnalysisResult 저장
            BiasAnalysisResultDto result = null;
        } catch (Exception e) {
            log.error("Analysis failed for job {}: {}", job.getId(), e.getMessage());
            job.updateStatus(JobStatus.FAILED);
            throw new AnalysisException(AnalysisErrorCode.ANALYSIS_JOB_FAILED, e.getMessage(), e);
        }

        return job;
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