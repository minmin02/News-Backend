package com.example.news.domain.personalization.dto;

import com.example.news.domain.personalization.enums.ScrapTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ScrapDto {

    public record CreateRequestDto(
            @NotBlank(message = "youtubeVideoId는 필수입니다")
            String youtubeVideoId
    ) {}

    public record ScrapResponseDto(
            Long scrapId,
            Long videoId,
            ScrapTargetType targetType,
            String youtubeVideoId,
            String title,
            String thumbnailUrl,
            String channelName,
            String originalUrl,
            Long viewCount,
            LocalDateTime publishedAt,
            LocalDateTime scrapCreatedAt
    ) {}
}
