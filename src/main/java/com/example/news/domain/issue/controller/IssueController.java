package com.example.news.domain.issue.controller;

import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.service.IssueService;
import com.example.news.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    // 국가별 이슈 영상 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<IssueSearchResponseDto>> search(
            @RequestParam String searchKeyword, // 파라미터로 한국어 키워드
            @RequestParam String countries, // 파라미터로 나라 (쉼표로 구분함) (ex, KR, JP, US)
            @RequestParam(defaultValue = "30") int days) { // 파라미터로 기한 일수 (default 30일)
        return ResponseEntity.ok(ApiResponse.ok(issueService.search(searchKeyword, countries, days)));
    }

    // 국가별 대표 영상 비교 결과 조회 (저장된 비교 결과 조회)
    // 예: /api/v1/issues/comparison?issueClusterId=1
    @GetMapping("/comparison")
    public ResponseEntity<ApiResponse<IssueComparisonResponseDto>> comparison(
            @RequestParam Long issueClusterId) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.comparison(issueClusterId)));
    }


}
