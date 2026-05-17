package com.example.news.domain.issue.service;

import com.example.news.domain.analysis.service.AnalysisService;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterItem;
import com.example.news.domain.issue.enums.ClusterStatus;
import com.example.news.domain.issue.enums.IssueClusterItemSourceType;
import com.example.news.domain.issue.enums.IssueClusterType;
import com.example.news.domain.issue.repository.IssueClusterItemRepository;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import com.example.news.global.event.VideoSearchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAnalysisTriggerService {

    private final IssueClusterRepository issueClusterRepository;
    private final IssueClusterItemRepository issueClusterItemRepository;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final AnalysisService analysisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleVideoSearched(VideoSearchedEvent event) {
        LocalDate today = LocalDate.now();

        IssueCluster cluster = issueClusterRepository.save(
                IssueCluster.builder()
                        .searchKeyword(event.keyword())
                        .normalizedKeyword(event.keyword().trim().toLowerCase())
                        .periodStartDate(today)
                        .periodEndDate(today)
                        .status(ClusterStatus.PENDING)
                        .clusterType(IssueClusterType.SEARCH_AUTO)
                        .build()
        );

        List<YoutubeVideo> videos = youtubeVideoRepository.findAllById(event.videoDbIds());
        for (YoutubeVideo video : videos) {
            String countryCode = video.getCountryCode() != null ? video.getCountryCode() : "KR";
            issueClusterItemRepository.save(
                    IssueClusterItem.builder()
                            .issueCluster(cluster)
                            .youtubeVideoId(video.getId())
                            .countryCode(countryCode)
                            .isRepresentative(false)
                            .sourceType(IssueClusterItemSourceType.AUTO)
                            .build()
            );
            analysisService.triggerAnalysisAsync(video.getId());
        }

        log.info("일반 검색 백그라운드 처리 완료 (keyword={}, videos={})", event.keyword(), videos.size());
    }
}
