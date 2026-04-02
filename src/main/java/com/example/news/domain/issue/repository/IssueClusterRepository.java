package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.enums.ClusterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IssueClusterRepository extends JpaRepository<IssueCluster, Long> {

    Optional<IssueCluster> findBySearchKeywordAndPeriodStartDateAndPeriodEndDate(
            String searchKeyword, LocalDate periodStartDate, LocalDate periodEndDate);

    List<IssueCluster> findByStatus(ClusterStatus status);
}
