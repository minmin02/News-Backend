package com.example.news.domain.personalization.converter;

import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.personalization.dto.ScrapDto;
import com.example.news.domain.personalization.entity.Scrap;

public class ScrapConverter {

    public static Scrap toScrap(Long userId, Long videoDbId) {
        return Scrap.builder()
                .userId(userId)
                .targetId(videoDbId)
                .targetType(com.example.news.domain.personalization.enums.ScrapTargetType.YOUTUBE_VIDEO)
                .build();
    }

    public static ScrapDto.ScrapResponseDto toScrapResponseDto(Scrap scrap, YoutubeVideo video) {
        return new ScrapDto.ScrapResponseDto(
                scrap.getId(),
                video.getId(),
                scrap.getTargetType(),
                video.getYoutubeVideoId(),
                video.getTitle(),
                video.getThumbnailUrl(),
                video.getChannelName(),
                video.getOriginalUrl(),
                video.getViewCount(),
                video.getPublishedAt(),
                scrap.getCreatedAt()
        );
    }
}
