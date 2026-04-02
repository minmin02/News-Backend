package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.IssueCluster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueClusterRepository extends JpaRepository<IssueCluster, Long> {
}
