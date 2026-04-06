package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.ComparisonResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComparisonResultRepository extends JpaRepository<ComparisonResult, Long> {

    List<ComparisonResult> findByIssueClusterId(Long issueClusterId);

    Optional<ComparisonResult> findTopByIssueClusterIdOrderByCreatedAtDesc(Long issueClusterId);
}
