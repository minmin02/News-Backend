package com.example.news.domain.issue.entity;

import com.example.news.domain.content.entity.YoutubeVideo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "issue_cluster_video")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueClusterVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_cluster_id")
    private IssueCluster issueCluster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "youtube_video_id")
    private YoutubeVideo youtubeVideo;

    private String countryCode;

    @Column(precision = 5, scale = 4)
    private BigDecimal similarityScore;

    private Boolean isRepresentative;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
