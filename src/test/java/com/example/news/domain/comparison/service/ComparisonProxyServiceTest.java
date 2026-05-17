package com.example.news.domain.comparison.service;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.comparison.dto.ComparisonVideoTargetResponse;
import com.example.news.domain.comparison.dto.collect.MultilingualKeywordExpandResponse;
import com.example.news.domain.comparison.exception.ComparisonException;
import com.example.news.domain.comparison.exception.code.ComparisonErrorCode;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparisonProxyServiceTest {

    @Mock
    YoutubeVideoRepository youtubeVideoRepository;

    @Mock
    BiasAnalysisResultRepository biasAnalysisResultRepository;

    ComparisonProxyService comparisonProxyService;

    AtomicReference<ClientRequest> capturedRequest;

    @BeforeEach
    void setUp() {
        capturedRequest = new AtomicReference<>();
        WebClient webClient = WebClient.builder()
                .exchangeFunction(okJsonExchange("""
                        {"issue_keywords":["economy"],"sections":[]}
                        """))
                .build();
        comparisonProxyService = new ComparisonProxyService(
                webClient,
                youtubeVideoRepository,
                biasAnalysisResultRepository
        );
        ReflectionTestUtils.setField(comparisonProxyService, "pythonBaseUrl", "http://localhost:8000");
    }

    @Test
    void getComparisonHome_callsPythonHomeEndpoint() {
        JsonNode result = comparisonProxyService.getComparisonHome(5);

        assertThat(result.get("issue_keywords").get(0).asText()).isEqualTo("economy");
        assertThat(capturedRequest.get().url().toString())
                .isEqualTo("http://localhost:8000/kg/comparison-home?limit=5");
    }

    @Test
    void searchVideos_trimsKeywordAndCallsPythonSearchEndpoint() {
        comparisonProxyService.searchVideos("  경제  ", 5);

        assertThat(capturedRequest.get().url().toString())
                .isEqualTo("http://localhost:8000/kg/search-videos?keyword=%EA%B2%BD%EC%A0%9C&limit=5");
    }

    @Test
    void getComparisonGraph_callsPythonGraphEndpoint() {
        comparisonProxyService.getComparisonGraph("abc123");

        assertThat(capturedRequest.get().url().toString())
                .isEqualTo("http://localhost:8000/kg/videos/abc123/comparison-graph");
    }

    @Test
    void searchVideos_throws400ErrorCode_whenKeywordBlank() {
        assertThatThrownBy(() -> comparisonProxyService.searchVideos(" ", 5))
                .isInstanceOfSatisfying(ComparisonException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ComparisonErrorCode.INVALID_COMPARISON_REQUEST));
    }

    @Test
    void getComparisonHome_throws400ErrorCode_whenLimitOutOfRange() {
        assertThatThrownBy(() -> comparisonProxyService.getComparisonHome(51))
                .isInstanceOfSatisfying(ComparisonException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ComparisonErrorCode.INVALID_COMPARISON_REQUEST));
    }

    @Test
    void getComparisonHome_throwsApiFailure_whenPythonReturnsError() {
        WebClient failingWebClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                .build();
        ComparisonProxyService service = new ComparisonProxyService(
                failingWebClient,
                youtubeVideoRepository,
                biasAnalysisResultRepository
        );
        ReflectionTestUtils.setField(service, "pythonBaseUrl", "http://localhost:8000");

        assertThatThrownBy(() -> service.getComparisonHome(5))
                .isInstanceOfSatisfying(ComparisonException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ComparisonErrorCode.COMPARISON_API_FAILED));
    }

    @Test
    void getAnalysisTarget_returnsInternalTargetIdAndAvailability() {
        YoutubeVideo video = YoutubeVideo.builder()
                .id(10L)
                .youtubeVideoId("abc123")
                .title("title")
                .build();
        when(youtubeVideoRepository.findByYoutubeVideoId("abc123")).thenReturn(Optional.of(video));
        when(biasAnalysisResultRepository.findTopByTargetIdAndTargetTypeOrderByCreatedAtDesc(10L, TargetType.YOUTUBE_VIDEO))
                .thenReturn(Optional.of(BiasAnalysisResult.builder().build()));

        ComparisonVideoTargetResponse result = comparisonProxyService.getAnalysisTarget("abc123");

        assertThat(result.youtubeVideoId()).isEqualTo("abc123");
        assertThat(result.targetId()).isEqualTo(10L);
        assertThat(result.analysisPath()).isEqualTo("/api/v1/analysis/10");
        assertThat(result.analysisAvailable()).isTrue();
        verify(youtubeVideoRepository).findByYoutubeVideoId("abc123");
    }

    @Test
    void getAnalysisTarget_throws404ErrorCode_whenVideoMissing() {
        when(youtubeVideoRepository.findByYoutubeVideoId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comparisonProxyService.getAnalysisTarget("missing"))
                .isInstanceOfSatisfying(ComparisonException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ComparisonErrorCode.COMPARISON_VIDEO_NOT_FOUND));
    }

    @Test
    void expandMultilingualKeywords_parsesNestedExpandedKeywords() {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(okJsonExchange("""
                        {
                          "requested_keyword":"트럼프 대만",
                          "expanded_keywords":{
                            "ko":["트럼프 대만"],
                            "en":["trump taiwan"],
                            "zh":["特朗普台湾"]
                          }
                        }
                        """))
                .build();
        ComparisonProxyService service = new ComparisonProxyService(
                webClient,
                youtubeVideoRepository,
                biasAnalysisResultRepository
        );
        ReflectionTestUtils.setField(service, "pythonBaseUrl", "http://localhost:8000");

        MultilingualKeywordExpandResponse result = service.expandMultilingualKeywords("트럼프 대만");

        assertThat(result.expandedKeywords().ko()).containsExactly("트럼프 대만");
        assertThat(result.expandedKeywords().en()).containsExactly("trump taiwan");
        assertThat(result.expandedKeywords().zh()).containsExactly("特朗普台湾");
    }

    private ExchangeFunction okJsonExchange(String body) {
        return request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .build());
        };
    }
}
