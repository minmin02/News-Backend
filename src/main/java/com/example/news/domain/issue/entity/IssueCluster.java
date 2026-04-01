package com.example.news.domain.issue.entity;

import com.example.news.domain.issue.enums.ClusterStatus;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 이슈 클러스터 — 동일 이슈를 다루는 영상 묶음의 대표 단위.
 * 검색 키워드와 기간을 기준으로 생성되며, 클러스터링 상태(PENDING/COMPLETED/FAIL)를 관리한다.
 * 하나의 IssueCluster에 여러 국가의 YoutubeVideo가 IssueClusterItem으로 연결된다.
 */
@Entity
@Table(name = "issue_cluster")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueCluster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ISSUE_CLUSTER_ID")
    private Long id;

    private String searchKeyword;

    private String normalizedKeyword;

    private LocalDate periodStartDate;

    private LocalDate periodEndDate;

    private String clusterLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClusterStatus status;
}