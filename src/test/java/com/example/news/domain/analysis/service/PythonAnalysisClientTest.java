package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.AnalyzeRequestDto;
import com.example.news.domain.analysis.dto.BiasAnalysisResultDto;
import com.example.news.domain.analysis.dto.SentenceInputDto;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.content.entity.YoutubeVideo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PythonAnalysisClientTest {

    @Test
    void analyzeWithFallback_rawSuccess_usesRawEndpoint() {
        StubPythonAnalysisClient client = new StubPythonAnalysisClient();
        client.configure(1);
        client.script("/analyze/raw", successResult());

        PythonAnalysisClient.PythonAnalysisCallResult result = client.analyzeWithFallback(transcript("첫 문장. 둘째 문장"), 1L);

        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.finalEndpoint()).isEqualTo("/analyze/raw");
        assertThat(client.calls("/analyze/raw")).isEqualTo(1);
        assertThat(client.calls("/analyze")).isZero();
    }

    @Test
    void analyzeWithFallback_raw5xx_thenFallbackAnalyzeSuccess() {
        StubPythonAnalysisClient client = new StubPythonAnalysisClient();
        client.configure(1);
        client.script("/analyze/raw", httpError(500));
        client.script("/analyze", successResult());

        PythonAnalysisClient.PythonAnalysisCallResult result = client.analyzeWithFallback(transcript("첫 문장. 둘째 문장"), 2L);

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.finalEndpoint()).isEqualTo("/analyze");
        assertThat(client.calls("/analyze/raw")).isEqualTo(1);
        assertThat(client.calls("/analyze")).isEqualTo(1);
    }

    @Test
    void analyzeWithFallback_raw429_thenFallbackAnalyzeSuccess() {
        StubPythonAnalysisClient client = new StubPythonAnalysisClient();
        client.configure(1);
        client.script("/analyze/raw", httpError(429));
        client.script("/analyze", successResult());

        PythonAnalysisClient.PythonAnalysisCallResult result = client.analyzeWithFallback(transcript("첫 문장. 둘째 문장"), 3L);

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.finalEndpoint()).isEqualTo("/analyze");
    }

    @Test
    void analyzeWithFallback_timeout_thenFallbackAnalyzeSuccess() {
        StubPythonAnalysisClient client = new StubPythonAnalysisClient();
        client.configure(1);
        client.script("/analyze/raw", new RuntimeException(new TimeoutException("timeout")));
        client.script("/analyze", successResult());

        PythonAnalysisClient.PythonAnalysisCallResult result = client.analyzeWithFallback(transcript("첫 문장. 둘째 문장"), 4L);

        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.finalEndpoint()).isEqualTo("/analyze");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422})
    void analyzeWithFallback_nonRetryable4xx_failsImmediately_withoutFallback(int status) {
        StubPythonAnalysisClient client = new StubPythonAnalysisClient();
        client.configure(3);
        client.script("/analyze/raw", httpError(status));

        assertThatThrownBy(() -> client.analyzeWithFallback(transcript("첫 문장. 둘째 문장"), 5L))
                .isInstanceOf(PythonAnalysisClient.PythonApiException.class);

        assertThat(client.calls("/analyze/raw")).isEqualTo(1);
        assertThat(client.calls("/analyze")).isZero();
    }

    @Test
    void analyzeWithFallback_assignsStableContentSentenceIdEqualToSentenceOrder() {
        StubPythonAnalysisClient client = new StubPythonAnalysisClient();
        client.configure(1);
        client.script("/analyze/raw", httpError(500));
        client.script("/analyze", successResult());

        client.analyzeWithFallback(transcript("첫 문장. 둘째 문장! 셋째 문장?"), 6L);

        AnalyzeRequestDto fallbackRequest = client.lastFallbackRequest();
        assertThat(fallbackRequest).isNotNull();
        assertThat(fallbackRequest.sentences()).isNotEmpty();
        for (SentenceInputDto sentence : fallbackRequest.sentences()) {
            assertThat(sentence.contentSentenceId()).isEqualTo((long) sentence.sentenceOrder());
        }
    }

    private static WebClientResponseException httpError(int status) {
        return WebClientResponseException.create(
                status, "error", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );
    }

    private static BiasAnalysisResultDto successResult() {
        return new BiasAnalysisResultDto(
                10L, 0.5, 0.4, 0.3, 0.2, null, null,
                "summary", "perspective", "evidence", "neutral",
                0.7, "score-evidence", Map.of("EMOTIONAL", 0.3),
                List.of(), List.of(), List.of(), List.of()
        );
    }

    private static YoutubeTranscript transcript(String rawText) {
        YoutubeVideo video = YoutubeVideo.builder()
                .id(10L)
                .youtubeVideoId("video-1")
                .title("테스트 영상")
                .countryCode("KR")
                .build();
        return YoutubeTranscript.builder()
                .id(20L)
                .youtubeVideo(video)
                .languageCode("ko")
                .transcriptText(rawText)
                .build();
    }

    private static class StubPythonAnalysisClient extends PythonAnalysisClient {
        private final Map<String, Deque<Object>> scripted = new HashMap<>();
        private final Map<String, Integer> callCount = new HashMap<>();
        private AnalyzeRequestDto lastFallbackRequest;

        StubPythonAnalysisClient() {
            super(org.mockito.Mockito.mock(org.springframework.web.reactive.function.client.WebClient.class));
            ReflectionTestUtils.setField(this, "pythonBaseUrl", "http://localhost:8000");
            ReflectionTestUtils.setField(this, "readTimeoutMs", 1000L);
            ReflectionTestUtils.setField(this, "initialBackoffMs", 1L);
            ReflectionTestUtils.setField(this, "maxBackoffMs", 2L);
        }

        void configure(int maxAttempts) {
            ReflectionTestUtils.setField(this, "maxAttempts", maxAttempts);
        }

        void script(String endpoint, Object... responses) {
            Deque<Object> queue = scripted.computeIfAbsent(endpoint, ignored -> new ArrayDeque<>());
            for (Object response : responses) {
                queue.add(response);
            }
        }

        int calls(String endpoint) {
            return callCount.getOrDefault(endpoint, 0);
        }

        AnalyzeRequestDto lastFallbackRequest() {
            return lastFallbackRequest;
        }

        @Override
        protected BiasAnalysisResultDto invokeEndpoint(String endpoint, Object requestBody) {
            callCount.put(endpoint, calls(endpoint) + 1);
            if ("/analyze".equals(endpoint) && requestBody instanceof AnalyzeRequestDto request) {
                lastFallbackRequest = request;
            }
            Deque<Object> queue = scripted.get(endpoint);
            if (queue == null || queue.isEmpty()) {
                throw new IllegalStateException("No scripted response for " + endpoint);
            }
            Object next = queue.removeFirst();
            if (next instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            return (BiasAnalysisResultDto) next;
        }

        @Override
        protected void sleep(long millis) {
            // no-op for test
        }
    }
}
