package com.example.news.domain.comparison.service;

import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.comparison.dto.ComparisonVideoTargetResponse;
import com.example.news.domain.comparison.dto.collect.MultilingualKeywordExpandResponse;
import com.example.news.domain.comparison.exception.ComparisonException;
import com.example.news.domain.comparison.exception.code.ComparisonErrorCode;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonProxyService {

    private final WebClient webClient;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final BiasAnalysisResultRepository biasAnalysisResultRepository;

    @Value("${python.base-url}")
    private String pythonBaseUrl;

    @PostConstruct
    void logPythonBaseUrlOnStartup() {
        log.info("pipeline=comparison_collect action=startup_config python_base_url={} event_time={}",
                pythonBaseUrl, java.time.OffsetDateTime.now());
    }

    public JsonNode getComparisonHome(int limit) {
        validateLimit(limit);
        try {
            return webClient.get()
                    .uri(UriComponentsBuilder.fromUriString(pythonBaseUrl + "/kg/comparison-home")
                            .queryParam("limit", limit)
                            .build()
                            .encode()
                            .toUri())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("[ComparisonProxy] /kg/comparison-home 호출 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ComparisonException(
                    ComparisonErrorCode.COMPARISON_API_FAILED,
                    "/kg/comparison-home 호출 실패",
                    e
            );
        }
    }

    public JsonNode searchVideos(@NonNull String keyword, int limit) {
        validateLimit(limit);
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isBlank()) {
            throw new ComparisonException(
                    ComparisonErrorCode.INVALID_COMPARISON_REQUEST,
                    "keyword는 비어 있을 수 없습니다."
            );
        }
        try {
            return webClient.get()
                    .uri(UriComponentsBuilder.fromUriString(pythonBaseUrl + "/kg/search-videos")
                            .queryParam("keyword", trimmedKeyword)
                            .queryParam("limit", limit)
                            .build()
                            .encode()
                            .toUri())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("[ComparisonProxy] /kg/search-videos 호출 실패 - keyword={}, status={}",
                    trimmedKeyword, e.getStatusCode());
            throw new ComparisonException(
                    ComparisonErrorCode.COMPARISON_API_FAILED,
                    "/kg/search-videos 호출 실패",
                    e
            );
        }
    }

    public JsonNode getComparisonGraph(String videoId) {
        try {
            return webClient.get()
                    .uri(pythonBaseUrl + "/kg/videos/" + videoId + "/comparison-graph")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("[ComparisonProxy] /kg/videos/{}/comparison-graph 호출 실패 - status={}",
                    videoId, e.getStatusCode());
            throw new ComparisonException(
                    ComparisonErrorCode.COMPARISON_API_FAILED,
                    "/kg/videos/{video_id}/comparison-graph 호출 실패",
                    e
            );
        }
    }

    public ComparisonVideoTargetResponse getAnalysisTarget(String youtubeVideoId) {
        YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
                .orElseThrow(() -> new ComparisonException(
                        ComparisonErrorCode.COMPARISON_VIDEO_NOT_FOUND,
                        "youtubeVideoId에 해당하는 영상을 찾을 수 없습니다: " + youtubeVideoId
                ));

        Long targetId = video.getId();
        boolean analysisAvailable = biasAnalysisResultRepository
                .findTopByTargetIdAndTargetTypeOrderByCreatedAtDesc(targetId, TargetType.YOUTUBE_VIDEO)
                .isPresent();

        return new ComparisonVideoTargetResponse(
                youtubeVideoId,
                targetId,
                "/api/v1/analysis/" + targetId,
                analysisAvailable
        );
    }

    public MultilingualKeywordExpandResponse expandMultilingualKeywords(String keywordKo) {
        log.info("pipeline=comparison_collect requested_keyword=\"{}\" python_base_url={} action=expand_keywords_request event_time={}",
                keywordKo, pythonBaseUrl, java.time.OffsetDateTime.now());
        try {
            Map<String, String> payload = Map.of("keyword_ko", keywordKo);
            log.info("pipeline=comparison_collect requested_keyword=\"{}\" python_base_url={} request_body={} event_time={}",
                    keywordKo, pythonBaseUrl, payload, java.time.OffsetDateTime.now());
            MultilingualKeywordExpandResponse response = webClient.post()
                    .uri(pythonBaseUrl + "/kg/expand-multilingual-keywords")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(MultilingualKeywordExpandResponse.class)
                    .block();

            if (response == null || response.expandedKeywords() == null) {
                throw new ComparisonException(
                        ComparisonErrorCode.COMPARISON_API_FAILED,
                        "다국어 확장 키워드 응답이 비어 있습니다."
                );
            }

            List<String> ko = normalizeKeywords(response.expandedKeywords().ko());
            List<String> en = normalizeKeywords(response.expandedKeywords().en());
            List<String> zh = normalizeKeywords(response.expandedKeywords().zh());

            log.info("pipeline=comparison_collect requested_keyword=\"{}\" python_base_url={} raw_response_requested_keyword=\"{}\" event_time={}",
                    keywordKo, pythonBaseUrl, response.requestedKeyword(), java.time.OffsetDateTime.now());
            log.info("pipeline=comparison_collect requested_keyword=\"{}\" python_base_url={} expanded_ko={} expanded_en={} expanded_zh={} fallback_applied=false fallback_reason=none event_time={}",
                    keywordKo, pythonBaseUrl, ko, en, zh, java.time.OffsetDateTime.now());

            return new MultilingualKeywordExpandResponse(
                    response.requestedKeyword(),
                    new MultilingualKeywordExpandResponse.ExpandedKeywords(ko, en, zh)
            );
        } catch (WebClientResponseException e) {
            log.warn("[ComparisonProxy] /kg/expand-multilingual-keywords 호출 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new ComparisonException(
                    ComparisonErrorCode.COMPARISON_API_FAILED,
                    "/kg/expand-multilingual-keywords 호출 실패",
                    e
            );
        }
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 50) {
            throw new ComparisonException(
                    ComparisonErrorCode.INVALID_COMPARISON_REQUEST,
                    "limit은 1~50 범위여야 합니다."
            );
        }
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) return List.of();
        return keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
