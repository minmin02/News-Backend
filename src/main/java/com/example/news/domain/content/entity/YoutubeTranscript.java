package com.example.news.domain.content.entity;

import com.example.news.domain.content.enums.TranscriptSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_transcript")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YoutubeTranscript {
    // 자막 텍스트 저장 테이블
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "youtube_video_id", nullable = false)
    private YoutubeVideo youtubeVideo;

    @Column(columnDefinition = "TEXT")
    private String transcriptText;

    @Enumerated(EnumType.STRING)
    private TranscriptSource transcriptSource;

    private String languageCode;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
