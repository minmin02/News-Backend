package com.example.news.domain.content.converter;

import com.example.news.domain.content.dto.YoutubeCommentDto;
import com.example.news.domain.content.dto.YoutubeTranscriptDto;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.entity.YoutubeComment;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.TranscriptSource;

import java.time.LocalDateTime;

public class YoutubeConverter {
    // 엔티티에서 DTO 변환 정적 메서드 모음.
    // (인스턴스화 불필요한 유틸 클래스)

    public static YoutubeVideoDto.VideoCard toVideoCard(YoutubeVideo video) {
        return YoutubeVideoDto.VideoCard.builder()
                .youtubeVideoId(video.getYoutubeVideoId())
                .title(video.getTitle())
                .thumbnailUrl(video.getThumbnailUrl())
                .channelName(video.getChannelName())
                .publishedAt(video.getPublishedAt())
                .viewCount(video.getViewCount())
                .durationSeconds(video.getDurationSeconds())
                .countryCode(video.getCountryCode())
                .build();
    }

    public static YoutubeVideoDto.VideoDetail toVideoDetail(YoutubeVideo video) {
        return YoutubeVideoDto.VideoDetail.builder()
                .youtubeVideoId(video.getYoutubeVideoId())
                .originalUrl(video.getOriginalUrl())
                .title(video.getTitle())
                .description(video.getDescription())
                .thumbnailUrl(video.getThumbnailUrl())
                .channelId(video.getChannelId())
                .channelName(video.getChannelName())
                .publishedAt(video.getPublishedAt())
                .countryCode(video.getCountryCode())
                .defaultLanguageCode(video.getDefaultLanguageCode())
                .durationSeconds(video.getDurationSeconds())
                .isEmbeddable(video.getIsEmbeddable())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .commentCount(video.getCommentCount())
                .build();
    }

    public static YoutubeCommentDto toCommentDto(YoutubeComment comment) {
        return YoutubeCommentDto.builder()
                .commentId(comment.getExternalCommentId())
                .authorName(comment.getAuthorName())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .publishedAt(comment.getPublishedAt())
                .isTopComment(comment.getIsTopComment())
                .build();
    }

    public static YoutubeTranscriptDto toTranscriptDto(YoutubeTranscript transcript) {
        return YoutubeTranscriptDto.builder()
                .youtubeVideoId(transcript.getYoutubeVideo().getYoutubeVideoId())
                .transcriptText(transcript.getTranscriptText())
                .transcriptSource(transcript.getTranscriptSource())
                .languageCode(transcript.getLanguageCode())
                .isAvailable(transcript.getTranscriptText() != null)
                .build();
    }

    public static YoutubeTranscriptDto toUnavailableTranscriptDto(String youtubeVideoId) {
        return YoutubeTranscriptDto.builder()
                .youtubeVideoId(youtubeVideoId)
                .isAvailable(false)
                .build();
    }
}
