package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.IssueClusterItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueClusterItemRepository extends JpaRepository<IssueClusterItem, Long> {

    List<IssueClusterItem> findByIssueClusterId(Long issueClusterId);

    List<IssueClusterItem> findByIssueClusterIdAndCountryCode(Long issueClusterId, String countryCode);

    Optional<IssueClusterItem> findByIssueClusterIdAndIsRepresentativeTrue(Long issueClusterId);
}