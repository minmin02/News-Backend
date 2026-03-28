package com.example.news.domain.issue.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class IssueBiasResponseDto {

    private List<BiasResult> results;

    @Getter
    @Builder
    public static class BiasResult {
        private String countryCode;
        private String youtubeVideoId;
        private Double overallBiasScore;
        private Double opinionScore;
        private Double emotionScore;
        private Double anonymousSourceScore;
        private Double headlineBodyGapScore;
        private Double neutralityScore;
        private String toneLabel;
        private String perspectiveSummary;
    }
}
