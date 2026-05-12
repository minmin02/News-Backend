package com.example.news.domain.comparison.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonProxyService {

    private final WebClient webClient;

    @Value("${python.base-url}")
    private String pythonBaseUrl;

    public Object getHome() {
        try {
            return webClient.get()
                    .uri(pythonBaseUrl + "/kg/comparison-home")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("[ComparisonProxy] /kg/comparison-home 호출 실패 - status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public Object searchVideos(String keyword) {
        try {
            return webClient.get()
                    .uri(pythonBaseUrl + "/kg/search-videos?keyword=" + keyword)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("[ComparisonProxy] /kg/search-videos 호출 실패 - keyword={}, status={}",
                    keyword, e.getStatusCode());
            throw e;
        }
    }

    public Object getVideoGraph(String videoId) {
        try {
            return webClient.get()
                    .uri(pythonBaseUrl + "/kg/videos/" + videoId + "/comparison-graph")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("[ComparisonProxy] /kg/videos/{}/comparison-graph 호출 실패 - status={}",
                    videoId, e.getStatusCode());
            throw e;
        }
    }
}
