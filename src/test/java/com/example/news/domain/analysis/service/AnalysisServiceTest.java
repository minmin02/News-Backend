package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.BiasAnalysisResultDto;
import com.example.news.domain.analysis.dto.ContentPreparedEventDto;
import com.example.news.domain.analysis.dto.SentenceInputDto;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.ContentSentence;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.AnalysisJobRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisKeywordRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.analysis.repository.BiasEvidenceRepository;
import com.example.news.domain.analysis.repository.ContentSentenceRepository;
import com.example.news.domain.analysis.repository.SentenceBiasLabelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
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
    WebClient webClient;

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
    void createAnalysisJob_returnsSuccessStatus() {
        // given
        ContentPreparedEventDto event = new ContentPreparedEventDto(1L, 10L, "KR", "ko", List.of());
        AnalysisJob saved = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        when(analysisJobRepository.save(any())).thenReturn(saved);

        BiasAnalysisResultDto dto = new BiasAnalysisResultDto(
                10L, 0.5, 0.4, 0.3, 0.2, null, null,
                "summary", "perspective", "evidence", "neutral",
                List.of(), List.of(), List.of()
        );
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BiasAnalysisResultDto.class)).thenReturn(Mono.just(dto));

        when(biasAnalysisResultRepository.save(any())).thenReturn(BiasAnalysisResult.builder().build());

        // when
        AnalysisJob result = analysisService.createAnalysisJob(event);

        // then
        assertThat(result.getStatus()).isEqualTo(JobStatus.SUCCESS);
        verify(analysisJobRepository).save(any(AnalysisJob.class));
    }

    @Test
    void createAnalysisJob_returnsFailedStatus_whenPythonThrows() {
        // given
        ContentPreparedEventDto event = new ContentPreparedEventDto(1L, 10L, "KR", "ko", List.of());
        AnalysisJob saved = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build();
        when(analysisJobRepository.save(any())).thenReturn(saved);
        when(webClient.post()).thenThrow(new RuntimeException("Python unavailable"));

        // when
        AnalysisJob result = analysisService.createAnalysisJob(event);

        // then
        assertThat(result.getStatus()).isEqualTo(JobStatus.FAILED);
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
