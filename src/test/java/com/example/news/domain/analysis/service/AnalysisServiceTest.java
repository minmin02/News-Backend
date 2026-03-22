package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.ContentPreparedEventDto;
import com.example.news.domain.analysis.dto.SentenceInputDto;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.entity.ContentSentence;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.AnalysisJobRepository;
import com.example.news.domain.analysis.repository.ContentSentenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    AnalysisJobRepository analysisJobRepository;

    @Mock
    ContentSentenceRepository contentSentenceRepository;

    @InjectMocks
    AnalysisService analysisService;

    @Test
    void createAnalysisJob_PENDING() {
        ContentPreparedEventDto event = new ContentPreparedEventDto(1L, 10L, "KR", "ko", List.of());
        AnalysisJob saved = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        when(analysisJobRepository.save(any())).thenReturn(saved);

        AnalysisJob result = analysisService.createAnalysisJob(event);

        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        verify(analysisJobRepository).save(any(AnalysisJob.class));
    }

    @Test
    void getSentenceInputs_sentenceOrder_sorting() {
        ContentSentence s1 = ContentSentence.builder()
                .targetId(10L)
                .sentenceOrder(1)
                .sentenceText("hello")
                .build();
        when(contentSentenceRepository.findAllByTargetIdOrderBySentenceOrder(10L))
                .thenReturn(List.of(s1));

        List<SentenceInputDto> result = analysisService.getSentenceInputs(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sentenceOrder()).isEqualTo(1);
        assertThat(result.get(0).sentenceText()).isEqualTo("hello");
    }
}