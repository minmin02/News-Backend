package com.example.news.domain.issue.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class IssuePopularVideosResponseDto {

    private List<VideoCard> videos;

    @Getter
    @Builder
    public static class VideoCard {
        private Long videoId;
        private String youtubeVideoId;
        private String title;
        private String channelName;
        private String thumbnailUrl;
        private String originalUrl;
        private LocalDateTime publishedAt;
        private Long viewCount;
        private Integer durationSeconds;
        private String countryCode;
    }
}
