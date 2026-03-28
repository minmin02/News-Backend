package com.example.news.domain.issue.controller;

import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.service.IssueService;
import com.example.news.global.response.ApiResponse;
import jakarta.validation.Valid;
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
            @RequestParam String searchKeyword,
            @RequestParam String countries,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.search(searchKeyword, countries, period)));
    }

    // 국가별 대표 영상 비교 결과 조회
    @GetMapping("/comparison")
    public ResponseEntity<ApiResponse<IssueComparisonResponseDto>> comparison(
            @RequestParam String searchKeyword,
            @RequestParam String countries,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.comparison(searchKeyword, countries, period)));
    }

    // 국가별 이슈 영상 편향 분석
    @PostMapping("/bias")
    public ResponseEntity<ApiResponse<IssueBiasResponseDto>> bias(
            @RequestBody @Valid IssueBiasRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.bias(request)));
    }

    // 인기 비교 이슈 영상 카드
    @GetMapping("/popular-videos")
    public ResponseEntity<ApiResponse<IssuePopularVideosResponseDto>> popularVideos(
            @RequestParam(defaultValue = "3") int size) {
        return ResponseEntity.ok(ApiResponse.ok(issueService.popularVideos(size)));
    }
}
