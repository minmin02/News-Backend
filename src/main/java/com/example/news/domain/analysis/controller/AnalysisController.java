package com.example.news.domain.analysis.controller;

import com.example.news.domain.analysis.dto.AnalysisJobResponse;
import com.example.news.domain.analysis.dto.AnalysisResultResponse;
import com.example.news.domain.analysis.dto.ContentPreparedEventDto;
import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.service.AnalysisService;
import com.example.news.domain.analysis.service.BiasAnalysisResultService;
import com.example.news.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final BiasAnalysisResultService biasAnalysisResultService;
    private final AnalysisService analysisService;

    /**
     * Content BC에서 분석 요청을 받아 Python 호출 + 결과 저장까지 동기로 처리하고 반환한다.
     * createAnalysisJob()이 block() 기반 동기 처리이므로 200 OK 반환.
     */
    @PostMapping("/analyze")
    public ApiResponse<AnalysisJobResponse> analyze(
            @RequestBody ContentPreparedEventDto event) {
        AnalysisJob job = analysisService.createAnalysisJob(event);
        return ApiResponse.ok(new AnalysisJobResponse(job.getId(), job.getStatus()));
    }

    @GetMapping("/{targetId}")
    public ApiResponse<AnalysisResultResponse> getAnalysisResult(
            @PathVariable Long targetId) {
        AnalysisResultResponse result = biasAnalysisResultService.getAnalysisResult(targetId);
        return ApiResponse.ok(result);
    }
}
