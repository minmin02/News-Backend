package com.example.news.domain.comparison.dto;

public record ComparisonVideoTargetResponse(
        String youtubeVideoId,
        Long targetId,
        String analysisPath,
        boolean analysisAvailable
) {
}
