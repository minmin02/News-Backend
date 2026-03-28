package com.example.news.domain.issue.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class IssueComparisonResponseDto {

    private String searchKeyword;
    private List<CountryResult> countries;

    @Getter
    @Builder
    public static class CountryResult {
        private String countryCode;
        private String countryName;
        private String languageLabel;
        private Long videoId;
        private String youtubeVideoId;
        private String title;
        private String channelName;
        private String thumbnailUrl;
        private String originalUrl;
        private LocalDateTime publishedAt;
        private Double overallBiasScore;
        private Double opinionScore;
        private Double emotionScore;
        private String toneLabel;
        private String perspectiveSummary;
        private String evidenceSummary;
        private ComparisonSummary comparison;
    }

    @Getter
    @Builder
    public static class ComparisonSummary {
        private String searchKeyword;
        private String perspectiveSummary;
        private String toneLabel;
        private String representativeChannelName;
        private List<String> coreKeywords;
    }
}
