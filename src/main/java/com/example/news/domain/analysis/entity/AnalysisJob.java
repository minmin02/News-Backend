package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.user.entity.User;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_job")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANALYSIS_JOB_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    // YoutubeVideo 엔티티의 pk
    private Long targetId;

    private String modelVersion;

    // 작업 대상 타입(유튜브 영상 1개 대상 or 같은 이슈로 묶인 비교 세트 전체)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    //작업 유형(음성텍스트 가져옴, 영상 요약, 편향석 분석, 여러 국가영상 비교분석)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime requestedAt;

    private LocalDateTime finishedAt;

    public void updateStatus(JobStatus newStatus) {
        this.status = newStatus;
    }

    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
    }

}