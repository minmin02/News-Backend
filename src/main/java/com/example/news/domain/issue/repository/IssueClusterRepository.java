package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.IssueCluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface IssueClusterRepository extends JpaRepository<IssueCluster, Long> {
    // 국가별 대표 영상 비교 결과 조회에서 사용
    // 같은 키워드와 기간 클러스터 중 가장 최근 것 조회
    Optional<IssueCluster> findTopBySearchKeywordAndPeriodStartDateAndPeriodEndDateOrderByCreatedAtDesc(
            String searchKeyword, LocalDate periodStartDate, LocalDate periodEndDate);
}
