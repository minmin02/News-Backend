package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueClusterVideoRepository extends JpaRepository<IssueClusterVideo, Long> {

    // 클러스터에 속한 모든 영상 조회
    List<IssueClusterVideo> findByIssueCluster(IssueCluster issueCluster);

}
