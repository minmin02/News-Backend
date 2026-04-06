package com.example.news.domain.issue.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class IssueSearchResponseDto {

    private Long issueClusterId;
    private String searchKeyword;
    private String clusterTitle;
    private String clusterSummary;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private List<VideoResult> results;

    @Getter
    @Builder
    public static class VideoResult {
        private Long videoId;
        private String youtubeVideoId;
        private String countryCode;
        private String title;
        private String channelName;
        private String thumbnailUrl;
        private LocalDateTime publishedAt;
        private Double similarityScore;
        private Boolean isRepresentative;
    }
}
