package com.example.news.domain.issue.service;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.BiasAnalysisKeywordRepository;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.analysis.service.AnalysisService;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.content.service.YoutubeSearchService;
import com.example.news.domain.issue.converter.IssueConverter;
import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.entity.ComparisonCountryItem;
import com.example.news.domain.issue.entity.ComparisonResult;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterItem;
import com.example.news.domain.issue.enums.ClusterStatus;
import com.example.news.domain.issue.exception.IssueErrorCode;
import com.example.news.domain.issue.exception.IssueException;
import com.example.news.domain.issue.repository.ComparisonCountryItemRepository;
import com.example.news.domain.issue.repository.ComparisonResultRepository;
import com.example.news.domain.issue.repository.IssueClusterItemRepository;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import com.example.news.domain.graph.service.IssueGraphSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final KeywordTranslationService keywordTranslationService;
    private final YoutubeSearchService youtubeSearchService;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final IssueClusterRepository issueClusterRepository;
    private final IssueClusterItemRepository issueClusterItemRepository;
    private final ComparisonResultRepository comparisonResultRepository;
    private final ComparisonCountryItemRepository comparisonCountryItemRepository;
    private final BiasAnalysisResultRepository biasAnalysisResultRepository;
    private final BiasAnalysisKeywordRepository biasAnalysisKeywordRepository;
    private final IssueGraphSyncService issueGraphSyncService;
    private final AnalysisService analysisService;

    // 국가별 이슈 영상 검색
    @Transactional
    public IssueSearchResponseDto search(String searchKeyword, String countries, int days) {
        List<String> countryList = IssueConverter.parseCountries(countries);
        LocalDate[] dates = IssueConverter.parsePeriod(days);
        LocalDate startDate = dates[0];
        LocalDate endDate = dates[1];

        // 국가별 키워드 번역
        Map<String, String> translatedKeywords = keywordTranslationService.translate(searchKeyword, countryList);

        // issue_cluster 생성 (검색 세션 한개를 의미함)
        IssueCluster cluster = issueClusterRepository.save(
                IssueCluster.builder()
                        .searchKeyword(searchKeyword)
                        .periodStartDate(startDate)
                        .periodEndDate(endDate)
                        .status(ClusterStatus.PENDING)
                        .build()
        );

        // 국가별 YouTube 검색 + issue_cluster_item 저장
        for (String countryCode : countryList) {
            String translatedKeyword = translatedKeywords.get(countryCode);
            String langCode = IssueConverter.getLanguageCode(countryCode);

            var videos = youtubeSearchService.searchByRegion(
                    translatedKeyword, countryCode, langCode, startDate, endDate);

            saveClusterItems(cluster, countryCode, videos);
        }

        // 저장된 아이템 조회 후 영상 정보 batch fetch
        List<IssueClusterItem> clusterItems = issueClusterItemRepository.findByIssueClusterId(cluster.getId());
        List<Long> videoIds = clusterItems.stream().map(IssueClusterItem::getYoutubeVideoId).toList();
        Map<Long, YoutubeVideo> videoMap = youtubeVideoRepository.findAllById(videoIds).stream()
                .collect(Collectors.toMap(YoutubeVideo::getId, v -> v));

        // 클러스터 내 모든 영상 백그라운드 분석 트리거 (커밋 이후 실행 보장)
        List<Long> triggerIds = videoIds;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                triggerIds.forEach(analysisService::triggerAnalysisAsync);
            }
        });

        IssueSearchResponseDto response = IssueConverter.toSearchResponse(cluster, clusterItems, videoMap);
        issueGraphSyncService.syncIssue(cluster, clusterItems, videoMap);
        return response;
    }

    // 국가별 대표 영상 비교 결과 조회 (저장된 비교 결과 조회)
    @Transactional(readOnly = true)
    public IssueComparisonResponseDto comparison(Long issueClusterId) {
        issueClusterRepository.findById(issueClusterId)
                .orElseThrow(() -> new IssueException(IssueErrorCode.ISSUE_CLUSTER_NOT_FOUND));

        ComparisonResult comparisonResult = comparisonResultRepository
                .findTopByIssueClusterIdOrderByCreatedAtDesc(issueClusterId)
                .orElseThrow(() -> new IssueException(IssueErrorCode.COMPARISON_RESULT_NOT_FOUND));

        List<ComparisonCountryItem> countryItems = comparisonCountryItemRepository
                .findByComparisonResultId(comparisonResult.getId());

        if (countryItems.isEmpty()) {
            return IssueComparisonResponseDto.builder()
                    .countries(List.of())
                    .build();
        }

        List<Long> videoIds = countryItems.stream()
                .map(ComparisonCountryItem::getRepresentativeVideoId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, YoutubeVideo> videoMap = youtubeVideoRepository.findAllById(videoIds).stream()
                .collect(Collectors.toMap(YoutubeVideo::getId, v -> v));

        Map<Long, BiasAnalysisResult> analysisResultMap = biasAnalysisResultRepository
                .findByTargetTypeAndTargetIdIn(TargetType.YOUTUBE_VIDEO, videoIds)
                .stream()
                .collect(Collectors.toMap(
                        BiasAnalysisResult::getTargetId,
                        r -> r,
                        (a, b) -> {
                            if (a.getCreatedAt() == null) return b;
                            if (b.getCreatedAt() == null) return a;
                            return a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b;
                        }
                ));

        List<IssueComparisonResponseDto.CountryResult> results = countryItems.stream()
                .map(item -> IssueConverter.toComparisonCountryResult(
                        item,
                        videoMap.get(item.getRepresentativeVideoId()),
                        analysisResultMap.get(item.getRepresentativeVideoId())
                ))
                .toList();

        return IssueComparisonResponseDto.builder()
                .countries(results)
                .build();
    }

    // 반대 관점 영상 도출: 같은 IssueCluster 내에서 opinionScore 차이가 가장 큰 영상 반환
    @Transactional(readOnly = true)
    public OpposingVideoResponseDto findOpposingVideo(Long videoId) {
        BiasAnalysisResult myResult = biasAnalysisResultRepository
                .findTopByTargetIdAndTargetTypeOrderByCreatedAtDesc(videoId, TargetType.YOUTUBE_VIDEO)
                .orElseThrow(() -> new IssueException(IssueErrorCode.ANALYSIS_NOT_COMPLETED));

        if (myResult.getOpinionScore() == null) {
            throw new IssueException(IssueErrorCode.ANALYSIS_NOT_COMPLETED);
        }
        double myOpinionScore = myResult.getOpinionScore();

        // 같은 클러스터 내 후보 수집
        Set<Long> clusterIds = issueClusterItemRepository.findByYoutubeVideoId(videoId)
                .stream()
                .map(item -> item.getIssueCluster().getId())
                .collect(Collectors.toSet());

        if (clusterIds.isEmpty()) {
            throw new IssueException(IssueErrorCode.OPPOSING_VIDEO_NOT_FOUND);
        }

        List<Long> candidateVideoIds = issueClusterItemRepository.findByIssueClusterIdIn(clusterIds)
                .stream()
                .map(IssueClusterItem::getYoutubeVideoId)
                .filter(id -> !id.equals(videoId))
                .distinct()
                .toList();

        if (candidateVideoIds.isEmpty()) {
            throw new IssueException(IssueErrorCode.OPPOSING_VIDEO_NOT_FOUND);
        }

        // opinionScore 차이 최대인 영상 선정
        BiasAnalysisResult opposing = biasAnalysisResultRepository
                .findByTargetTypeAndTargetIdIn(TargetType.YOUTUBE_VIDEO, candidateVideoIds)
                .stream()
                .filter(r -> r.getOpinionScore() != null)
                .max(Comparator
                        .comparingDouble((BiasAnalysisResult r) ->
                                Math.abs(myOpinionScore - r.getOpinionScore()))
                        .thenComparingDouble(r -> r.getOverallBiasScore() != null ? r.getOverallBiasScore() : 0.0))
                .orElseThrow(() -> new IssueException(IssueErrorCode.OPPOSING_VIDEO_NOT_FOUND));

        YoutubeVideo opposingVideo = youtubeVideoRepository.findById(opposing.getTargetId())
                .orElseThrow(() -> new IssueException(IssueErrorCode.VIDEO_NOT_FOUND));

        List<String> keywords = biasAnalysisKeywordRepository
                .findAllByBiasAnalysisResultId(opposing.getId())
                .stream()
                .map(k -> k.getKeywordText())
                .toList();

        double opinionGap = Math.abs(myOpinionScore - opposing.getOpinionScore());

        return OpposingVideoResponseDto.builder()
                .youtubeVideoId(opposingVideo.getYoutubeVideoId())
                .title(opposingVideo.getTitle())
                .channelName(opposingVideo.getChannelName())
                .summaryText(opposing.getSummaryText())
                .opinionScore(opposing.getOpinionScore())
                .overallBiasScore(opposing.getOverallBiasScore())
                .opinionGap(opinionGap)
                .scoreEvidence(opposing.getScoreEvidence())
                .analysisKeywords(keywords)
                .build();
    }

    private void saveClusterItems(IssueCluster cluster, String countryCode,
                                  List<com.example.news.domain.content.dto.YoutubeVideoDto.VideoCard> videoCards) {
        for (var card : videoCards) {
            youtubeVideoRepository.findByYoutubeVideoId(card.getYoutubeVideoId())
                    .ifPresent(video -> issueClusterItemRepository.save(
                            IssueClusterItem.builder()
                                    .issueCluster(cluster)
                                    .youtubeVideoId(video.getId())
                                    .countryCode(countryCode)
                                    .isRepresentative(false)
                                    .build()
                    ));
        }
    }
}
