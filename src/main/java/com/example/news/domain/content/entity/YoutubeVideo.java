package com.example.news.domain.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_video")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeVideo {
    // 유튜브 영상 정보 테이블
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_source_id")
    private ContentSource contentSource;

    @Column(unique = true, nullable = false)
    private String youtubeVideoId;

    @Column(unique = true)
    private String originalUrl;

    private String channelId;

    private String channelName;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailUrl;

    private LocalDateTime publishedAt;

    private LocalDateTime collectedAt;

    private String countryCode;

    private String defaultLanguageCode;

    private Integer durationSeconds;

    private Boolean isEmbeddable;

    private Long viewCount;

    private Long likeCount;

    private Long commentCount;
}
