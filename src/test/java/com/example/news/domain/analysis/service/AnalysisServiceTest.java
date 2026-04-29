package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.BiasAnalysisResultDto;
import com.example.news.domain.analysis.dto.EvidenceResultDto;
import com.example.news.domain.analysis.dto.KeywordResultDto;
import com.example.news.domain.analysis.dto.SentenceInputDto;
import com.example.news.domain.analysis.dto.SentenceLabelResultDto;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.ContentSentence;
import com.example.news.domain.analysis.entity.HighlightResult;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.SentenceTargetType;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.event.AnalysisCompletedEvent;
import com.example.news.domain.analysis.repository.AnalysisJobRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisKeywordRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.analysis.repository.BiasEvidenceRepository;
import com.example.news.domain.analysis.repository.ContentSentenceRepository;
import com.example.news.domain.analysis.repository.HighlightResultRepository;
import com.example.news.domain.analysis.repository.HighlightSpanRepository;
import com.example.news.domain.analysis.repository.SentenceBiasLabelRepository;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.content.entity.YoutubeVideo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    AnalysisJobRepository analysisJobRepository;

    @Mock
    ContentSentenceRepository contentSentenceRepository;

    @Mock
    BiasAnalysisResultRepository biasAnalysisResultRepository;

    @Mock
    BiasAnalysisKeywordRepository biasAnalysisKeywordRepository;

    @Mock
    SentenceBiasLabelRepository sentenceBiasLabelRepository;

    @Mock
    BiasEvidenceRepository biasEvidenceRepository;

    @Mock
    HighlightResultRepository highlightResultRepository;

    @Mock
    HighlightSpanRepository highlightSpanRepository;

    @Mock
    PythonAnalysisClient pythonAnalysisClient;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    AnalysisService analysisService;

    @Test
    void createAnalysisJobFromRawText_returnsSuccessStatus() {
        YoutubeTranscript transcript = transcript();

        AnalysisJob saved = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        when(analysisJobRepository.save(any())).thenReturn(saved);

        BiasAnalysisResultDto pythonResult = biasResultDto();
        when(pythonAnalysisClient.analyzeWithFallback(any(), any()))
                .thenReturn(new PythonAnalysisClient.PythonAnalysisCallResult(
                        pythonResult, false, "/analyze/raw", List.of()
                ));

        ContentSentence mappedSentence = ContentSentence.builder()
                .targetId(1L)
                .targetType(SentenceTargetType.YOUTUBE_TRANSCRIPT)
                .sentenceOrder(1)
                .sentenceText("문장 1")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(mappedSentence, "id", 100L);
        when(contentSentenceRepository.saveAll(anyList())).thenReturn(List.of(mappedSentence));
        when(contentSentenceRepository.getReferenceById(100L)).thenReturn(mappedSentence);

        BiasAnalysisResult savedResult = BiasAnalysisResult.builder().build();
        org.springframework.test.util.ReflectionTestUtils.setField(savedResult, "id", 77L);
        when(biasAnalysisResultRepository.save(any())).thenReturn(savedResult);
        when(highlightResultRepository.save(any()))
                .thenReturn(HighlightResult.builder().biasAnalysisResult(savedResult).build());

        AnalysisJob result = analysisService.createAnalysisJobFromRawText(transcript);

        assertThat(result.getStatus()).isEqualTo(JobStatus.SUCCESS);
        verify(highlightResultRepository).save(any(HighlightResult.class));
        verify(highlightSpanRepository).saveAll(anyList());
        verify(eventPublisher).publishEvent(any(AnalysisCompletedEvent.class));
    }

    @Test
    void createAnalysisJobFromRawText_returnsFailedStatus_whenPythonThrows() {
        YoutubeTranscript transcript = transcript();

        AnalysisJob saved = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        when(analysisJobRepository.save(any())).thenReturn(saved);
        when(pythonAnalysisClient.analyzeWithFallback(any(), any())).thenThrow(new RuntimeException("Python unavailable"));

        AnalysisJob result = analysisService.createAnalysisJobFromRawText(transcript);

        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        verifyNoInteractions(eventPublisher);
    }

    private YoutubeTranscript transcript() {
        YoutubeVideo video = YoutubeVideo.builder()
                .id(10L)
                .title("테스트 제목")
                .countryCode("KR")
                .youtubeVideoId("abc123")
                .build();
        return YoutubeTranscript.builder()
                .id(1L)
                .youtubeVideo(video)
                .languageCode("ko")
                .transcriptText("문장 1. 문장 2.")
                .build();
    }

    private BiasAnalysisResultDto biasResultDto() {
        SentenceInputDto sentence = new SentenceInputDto(1L, "문장 1", 1, null, null);
        SentenceLabelResultDto label = new SentenceLabelResultDto(1L, "EMOTIONALLY_LOADED", 0.91, "YELLOW", "최악", 0, 2, "최악");
        EvidenceResultDto evidence = new EvidenceResultDto(1L, "ANONYMOUS_SOURCE", "제목", "설명", "원문", 0.8);
        KeywordResultDto keyword = new KeywordResultDto("편향", "EMOTION", 0.5);
        return new BiasAnalysisResultDto(
                10L, 0.5, 0.4, 0.3, 0.2, null, null,
                "summary", "perspective", "evidence", "neutral",
                61.89, "주관적 문장 비율 57%", Map.of("OPINION", 0.4, "EMOTIONAL", 0.3),
                List.of(sentence), List.of(keyword), List.of(label), List.of(evidence)
        );
    }
}
