package com.example.news.domain.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_comment")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeComment {
    // 댓글 저장 테이블
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "youtube_video_id", nullable = false)
    private YoutubeVideo youtubeVideo;

    @Column(unique = true, nullable = false)
    private String externalCommentId;

    private String authorName;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Long likeCount;

    private LocalDateTime publishedAt;

    @Builder.Default
    private Boolean isTopComment = false;
}
