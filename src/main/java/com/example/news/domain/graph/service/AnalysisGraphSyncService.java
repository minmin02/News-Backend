package com.example.news.domain.graph.service;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.event.AnalysisCompletedEvent;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.graph.node.AnalysisResultNode;
import com.example.news.domain.graph.node.VideoNode;
import com.example.news.domain.graph.repository.AnalysisResultNodeRepository;
import com.example.news.domain.graph.repository.VideoNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisGraphSyncService {

    private static final String SUCCESS_STATUS = "SUCCESS";

    private final BiasAnalysisResultRepository biasAnalysisResultRepository;
    private final AnalysisResultNodeRepository analysisResultNodeRepository;
    private final VideoNodeRepository videoNodeRepository;

    @Async
    @EventListener
    public void syncAnalysisResult(AnalysisCompletedEvent event) {
        if (!SUCCESS_STATUS.equals(event.status())) {
            return;
        }

        try {
            VideoNode videoNode = videoNodeRepository.findByTargetId(event.targetId())
                    .orElse(null);
            if (videoNode == null) {
                logPipelineFailure("analysis", null, event.targetId(), null,
                        "video node not found for target_id=" + event.targetId(), false);
                return;
            }

            BiasAnalysisResult result = biasAnalysisResultRepository.findById(event.biasAnalysisResultId())
                    .orElse(null);
            if (result == null) {
                logPipelineFailure("analysis", videoNode.getYoutubeVideoId(), event.targetId(), null,
                        "bias analysis result not found id=" + event.biasAnalysisResultId(), false);
                return;
            }

            AnalysisResultNode analysisResultNode = analysisResultNodeRepository
                    .findById(event.biasAnalysisResultId())
                    .orElseGet(() -> AnalysisResultNode.builder()
                            .analysisResultId(event.biasAnalysisResultId())
                            .build());

            analysisResultNode.setOpinionScore(result.getOpinionScore());
            analysisResultNode.setOverallBiasScore(result.getOverallBiasScore());
            analysisResultNode.setStatus(SUCCESS_STATUS);
            analysisResultNodeRepository.save(analysisResultNode);

            // 재분석 시 최신 결과 하나만 유지
            videoNode.setAnalysisResult(analysisResultNode);
            videoNodeRepository.save(videoNode);
        } catch (Exception e) {
            logPipelineFailure("analysis", null, event.targetId(), null, e.getMessage(), true);
            log.warn("[GraphSync] AnalysisResult 동기화 예외", e);
        }
    }

    private void logPipelineFailure(String pipeline,
                                    String videoId,
                                    Long targetId,
                                    Long issueId,
                                    String reason,
                                    boolean hasStackTrace) {
        log.warn(
                "pipeline={} video_id={} target_id={} issue_id={} reason=\"{}\" stacktrace={} event_time={}",
                pipeline,
                videoId,
                targetId,
                issueId,
                reason,
                hasStackTrace,
                OffsetDateTime.now()
        );
    }
}
