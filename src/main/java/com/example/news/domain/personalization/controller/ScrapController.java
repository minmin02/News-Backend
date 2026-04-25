package com.example.news.domain.personalization.controller;

import com.example.news.domain.personalization.dto.ScrapDto;
import com.example.news.domain.personalization.service.ScrapService;
import com.example.news.global.response.ApiResponse;
import com.example.news.global.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping
    public ApiResponse<Void> save(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody @Valid ScrapDto.CreateRequestDto request) {
        scrapService.save(userDetails.getUserId(), request);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<ScrapDto.ScrapResponseDto>> getMyScraps(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResponse.ok(scrapService.getMyScraps(userDetails.getUserId()));
    }

    @DeleteMapping("/{scrapId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long scrapId) {
        scrapService.delete(userDetails.getUserId(), scrapId);
        return ApiResponse.ok();
    }
}
