package com.example.news.domain.issue.service;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.content.service.YoutubeSearchService;
import com.example.news.domain.issue.converter.IssueConverter;
import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterItem;
import com.example.news.domain.issue.enums.ClusterStatus;
import com.example.news.domain.issue.enums.IssueErrorCode;
import com.example.news.domain.issue.repository.IssueClusterItemRepository;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import com.example.news.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
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

    // 국가별 대표 영상 비교 결과 조회 (선택한 영상 ID 직접 지정)
    @Transactional(readOnly = true)
    public IssueComparisonResponseDto comparison(Map<String, String> videoIds) {
        List<IssueComparisonResponseDto.CountryResult> results = new ArrayList<>();

        // Map<국가 코드, youtubeVideoId>를 받아서 각 영상을 db에서 조회
        // 이때 bias_analysis_result 가 없으면 null 반환 (지금은 아마 다 null 반환해야 정상임)
        for (Map.Entry<String, String> entry : videoIds.entrySet()) {
            String countryCode = entry.getKey();
            String youtubeVideoId = entry.getValue();

            YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
                    .orElseThrow(() -> new CustomException(IssueErrorCode.VIDEO_NOT_FOUND));

            BiasAnalysisResult bias = biasAnalysisResultRepository
                    .findByTargetIdAndTargetType(video.getId(), TargetType.YOUTUBE_VIDEO)
                    .orElse(null);

            results.add(IssueConverter.toComparisonCountryResult(countryCode, video, bias));
        }

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
