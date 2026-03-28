package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterVideo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IssueClusterVideoRepository extends JpaRepository<IssueClusterVideo, Long> {

    // 클러스터에 속한 모든 영상 조회
    List<IssueClusterVideo> findByIssueCluster(IssueCluster issueCluster);

    // 대표 영상 조회
    Optional<IssueClusterVideo> findFirstByIssueClusterAndCountryCodeAndIsRepresentativeTrue(
            IssueCluster issueCluster, String countryCode);

    // 대표 영상 없을 때 fallback
    Optional<IssueClusterVideo> findFirstByIssueClusterAndCountryCode(
            IssueCluster issueCluster, String countryCode);

    // iew_count 내림차순 상위 N개
    @Query("SELECT icv FROM IssueClusterVideo icv JOIN FETCH icv.youtubeVideo yv ORDER BY yv.viewCount DESC NULLS LAST")
    List<IssueClusterVideo> findTopByViewCountDesc(Pageable pageable);
}
