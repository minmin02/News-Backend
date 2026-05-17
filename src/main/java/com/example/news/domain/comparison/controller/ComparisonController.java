package com.example.news.domain.comparison.controller;

import com.example.news.domain.comparison.dto.ComparisonVideoTargetResponse;
import com.example.news.domain.comparison.dto.collect.CollectMultilingualRequest;
import com.example.news.domain.comparison.dto.collect.CollectMultilingualResponse;
import com.example.news.domain.comparison.service.ComparisonCollectService;
import com.example.news.domain.comparison.service.ComparisonProxyService;
import com.example.news.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comparison")
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonProxyService comparisonProxyService;
    private final ComparisonCollectService comparisonCollectService;

    // Python /kg/comparison-home 그대로 전달
    @GetMapping("/home")
    public ApiResponse<Object> getHome(@RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(comparisonProxyService.getComparisonHome(limit));
    }

    // Python /kg/search-videos?keyword= 그대로 전달
    @GetMapping("/search")
    public ApiResponse<Object> searchVideos(@RequestParam String keyword,
                                            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok(comparisonProxyService.searchVideos(keyword, limit));
    }

    // Python /kg/videos/{videoId}/comparison-graph 그대로 전달
    @GetMapping("/videos/{videoId}/graph")
    public ApiResponse<Object> getVideoGraph(@PathVariable String videoId) {
        return ApiResponse.ok(comparisonProxyService.getComparisonGraph(videoId));
    }

    // YouTube video ID → 내부 PK 변환 (그래프 노드 클릭 후 분석 결과 이동용)
    @GetMapping("/videos/{videoId}/target")
    public ApiResponse<ComparisonVideoTargetResponse> getTarget(@PathVariable String videoId) {
        return ApiResponse.ok(comparisonProxyService.getAnalysisTarget(videoId));
    }

    @PostMapping("/collect-multilingual")
    public ApiResponse<CollectMultilingualResponse> collectMultilingual(
            @Valid @RequestBody CollectMultilingualRequest request
    ) {
        return ApiResponse.ok(comparisonCollectService.collectMultilingual(request));
    }
}
