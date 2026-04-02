package com.example.news.domain.issue.entity;

import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 국가 간 비교 결과 — 특정 IssueCluster에 대해 Python FastAPI가 반환한 비교 분석 결과.
 * 비교 키워드, 분석 기간, 최종 응답 JSON(snapshotJson)을 통째로 보관하여
 * C-파트 조회 API에서 재가공 없이 제공하거나 ComparisonCountryItem으로 파싱해 서빙한다.
 */
@Entity
@Table(name = "comparison_result")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPARISON_RESULT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_cluster_id")
    private IssueCluster issueCluster;

    private String keyword;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    @Column(columnDefinition = "TEXT")
    private String snapshotJson;
}