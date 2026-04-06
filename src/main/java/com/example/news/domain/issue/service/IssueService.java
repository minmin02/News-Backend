package com.example.news.domain.issue.service;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        return IssueConverter.toSearchResponse(cluster, clusterItems, videoMap);
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
