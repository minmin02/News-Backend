package com.example.news.domain.graph.service;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.event.AnalysisCompletedEvent;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.graph.node.AnalysisResultNode;
import com.example.news.domain.graph.node.VideoNode;
import com.example.news.domain.graph.repository.AnalysisResultNodeRepository;
import com.example.news.domain.graph.repository.VideoNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisGraphSyncServiceTest {

    @Mock
    BiasAnalysisResultRepository biasAnalysisResultRepository;

    @Mock
    AnalysisResultNodeRepository analysisResultNodeRepository;

    @Mock
    VideoNodeRepository videoNodeRepository;

    @InjectMocks
    AnalysisGraphSyncService analysisGraphSyncService;

    @Test
    void syncAnalysisResult_linksLatestAnalysisToVideo() {
        AnalysisCompletedEvent event = new AnalysisCompletedEvent(
                10L,
                20L,
                30L,
                "SUCCESS",
                0.4,
                List.of("k1"),
                "summary",
                Map.of("political", 0.3)
        );

        VideoNode videoNode = VideoNode.builder()
                .youtubeVideoId("yt-1")
                .targetId(10L)
                .build();

        BiasAnalysisResult result = BiasAnalysisResult.builder()
                .id(30L)
                .opinionScore(0.6)
                .overallBiasScore(0.4)
                .build();

        when(videoNodeRepository.findByTargetId(10L)).thenReturn(Optional.of(videoNode));
        when(biasAnalysisResultRepository.findById(30L)).thenReturn(Optional.of(result));
        when(analysisResultNodeRepository.findById(30L)).thenReturn(Optional.empty());

        analysisGraphSyncService.syncAnalysisResult(event);

        ArgumentCaptor<AnalysisResultNode> analysisCaptor = ArgumentCaptor.forClass(AnalysisResultNode.class);
        verify(analysisResultNodeRepository).save(analysisCaptor.capture());

        AnalysisResultNode savedNode = analysisCaptor.getValue();
        assertThat(savedNode.getAnalysisResultId()).isEqualTo(30L);
        assertThat(savedNode.getOpinionScore()).isEqualTo(0.6);
        assertThat(savedNode.getOverallBiasScore()).isEqualTo(0.4);
        assertThat(savedNode.getStatus()).isEqualTo("SUCCESS");

        verify(videoNodeRepository).save(any(VideoNode.class));
        assertThat(videoNode.getAnalysisResult()).isNotNull();
        assertThat(videoNode.getAnalysisResult().getAnalysisResultId()).isEqualTo(30L);
    }

    @Test
    void syncAnalysisResult_skipsWhenStatusIsNotSuccess() {
        AnalysisCompletedEvent event = new AnalysisCompletedEvent(
                10L,
                20L,
                30L,
                "FAIL",
                0.1,
                List.of(),
                "",
                Map.of()
        );

        analysisGraphSyncService.syncAnalysisResult(event);

        verify(videoNodeRepository, never()).findByTargetId(any());
        verify(analysisResultNodeRepository, never()).save(any());
    }
}
