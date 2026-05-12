package com.example.news.domain.graph.service;

import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.graph.node.IssueNode;
import com.example.news.domain.graph.repository.IssueNodeRepository;
import com.example.news.domain.graph.repository.VideoNodeRepository;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueGraphSyncService {

    private final IssueNodeRepository issueNodeRepository;
    private final VideoNodeRepository videoNodeRepository;

    // videoMap: IssueService.search()에서 이미 조회된 Map<PostgreSQL PK, YoutubeVideo> — 중복 쿼리 없음
    @Async
    public void syncIssue(IssueCluster cluster,
                          List<IssueClusterItem> clusterItems,
                          Map<Long, YoutubeVideo> videoMap) {
        try {
            // Issue 노드 upsert — keyword, name 둘 다 저장 (Python이 둘 다 참조)
            IssueNode issueNode = issueNodeRepository.findById(cluster.getId())
                    .orElseGet(() -> IssueNode.builder()
                            .clusterId(cluster.getId())
                            .searchKeyword(cluster.getSearchKeyword())
                            .name(cluster.getSearchKeyword())
                            .periodStartDate(cluster.getPeriodStartDate())
                            .periodEndDate(cluster.getPeriodEndDate())
                            .build());
            issueNodeRepository.save(issueNode);

            // 각 영상 VideoNode에 PART_OF 관계 연결 (addIssue 내부에서 중복 방지)
            for (IssueClusterItem item : clusterItems) {
                YoutubeVideo video = videoMap.get(item.getYoutubeVideoId());
                if (video == null) continue;

                videoNodeRepository.findById(video.getYoutubeVideoId()).ifPresent(videoNode -> {
                    videoNode.addIssue(issueNode);
                    videoNodeRepository.save(videoNode);
                });
            }

        } catch (Exception e) {
            log.warn("[GraphSync] Issue 동기화 실패 - clusterId={}, error={}",
                    cluster.getId(), e.getMessage());
        }
    }
}
