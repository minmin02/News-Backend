package com.example.news.domain.content.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "youtube_video_keyword")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeVideoKeyword {
    // 영상-키워드 연결 테이블 (다대다 중간 테이블로 사용)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "youtube_video_id", nullable = false)
    private YoutubeVideo youtubeVideo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    @Builder.Default
    private Double weight = 1.0;
}
