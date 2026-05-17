package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.BiasAnalysisResultResponse;
import com.example.news.domain.analysis.dto.SentenceResultResponse;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.ContentSentence;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
    WebClient webClient;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @InjectMocks
    AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(analysisService, "pythonBaseUrl", "http://localhost:8000");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createAnalysisJobFromRawText_returnsSuccessStatus() {
        AnalysisJob saved = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        when(analysisJobRepository.save(any())).thenReturn(saved);

        BiasAnalysisResultResponse response = new BiasAnalysisResultResponse(
                10L,
                "YOUTUBE_VIDEO",
                1L,
                0.5,
                0.4,
                0.3,
                0.2,
                null,
                null,
                null,
                null,
                "reason",
                "summary",
                0.7,
                "evidence",
                Map.of("OPINION", 0.4),
                List.of(),
                List.of(),
                List.of(),
                List.of(new SentenceResultResponse(1L, "hello", 1))
        );
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BiasAnalysisResultResponse.class)).thenReturn(Mono.just(response));

        BiasAnalysisResult savedResult = BiasAnalysisResult.builder().build();
        when(biasAnalysisResultRepository.save(any())).thenReturn(savedResult);
        when(contentSentenceRepository.saveAll(anyList())).thenReturn(List.of(
                ContentSentence.builder()
                        .id(100L)
                        .targetId(1L)
                        .targetType(SentenceTargetType.YOUTUBE_TRANSCRIPT)
                        .sentenceOrder(1)
                        .sentenceText("hello")
                        .build()
        ));

        AnalysisJob result = analysisService.createAnalysisJobFromRawText(transcript());

        assertThat(result.getStatus()).isEqualTo(JobStatus.SUCCESS);
        verify(analysisJobRepository).save(any(AnalysisJob.class));
        verify(biasAnalysisResultRepository).save(any(BiasAnalysisResult.class));
        verify(eventPublisher).publishEvent(any(AnalysisCompletedEvent.class));
    }

    @Test
    void createAnalysisJobFromRawText_returnsFailedStatus_whenPythonThrows() {
        AnalysisJob saved = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        when(analysisJobRepository.save(any())).thenReturn(saved);
        when(webClient.post()).thenThrow(new RuntimeException("Python unavailable"));

        AnalysisJob result = analysisService.createAnalysisJobFromRawText(transcript());

        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
        verifyNoInteractions(eventPublisher);
    }

    private YoutubeTranscript transcript() {
        YoutubeVideo video = YoutubeVideo.builder()
                .id(10L)
                .youtubeVideoId("abc123")
                .title("테스트 제목")
                .countryCode("KR")
                .build();
        return YoutubeTranscript.builder()
                .id(1L)
                .youtubeVideo(video)
                .languageCode("ko")
                .transcriptText("hello")
                .build();
    }
}
