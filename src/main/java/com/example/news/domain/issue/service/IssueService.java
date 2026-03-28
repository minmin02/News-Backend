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
import com.example.news.domain.issue.entity.IssueClusterVideo;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import com.example.news.domain.issue.repository.IssueClusterVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final KeywordTranslationService keywordTranslationService;
    private final YoutubeSearchService youtubeSearchService;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final IssueClusterRepository issueClusterRepository;
    private final IssueClusterVideoRepository issueClusterVideoRepository;
    private final BiasAnalysisResultRepository biasAnalysisResultRepository;

    // 국가별 이슈 영상 검색
    @Transactional
    public IssueSearchResponseDto search(String searchKeyword, String countries, String period) {
        List<String> countryList = parseCountries(countries);
        LocalDate[] dates = parsePeriod(period);
        LocalDate startDate = dates[0];
        LocalDate endDate = dates[1];

        // 1. 국가별 키워드 번역
        Map<String, String> translatedKeywords = keywordTranslationService.translate(searchKeyword, countryList);

        // 2. issue_cluster 생성
        IssueCluster cluster = issueClusterRepository.save(
                IssueCluster.builder()
                        .searchKeyword(searchKeyword)
                        .periodStartDate(startDate)
                        .periodEndDate(endDate)
                        .build()
        );

        // 3. 국가별 YouTube 검색 + issue_cluster_video 저장
        for (String countryCode : countryList) {
            String translatedKeyword = translatedKeywords.get(countryCode);
            String langCode = KeywordTranslationService.getLanguageCode(countryCode);

            var videos = youtubeSearchService.searchByRegion(
                    translatedKeyword, countryCode, langCode, startDate, endDate);

            saveClusterVideos(cluster, countryCode, videos);
        }

        // 4. 저장된 영상 flat 리스트로 반환
        List<IssueClusterVideo> clusterVideos = issueClusterVideoRepository.findByIssueCluster(cluster);
        return IssueConverter.toSearchResponse(cluster, clusterVideos);
    }

    // 국가별 대표 영상 비교 결과 조회
    @Transactional(readOnly = true)
    public IssueComparisonResponseDto comparison(String searchKeyword, String countries, String period) {
        List<String> countryList = parseCountries(countries);
        LocalDate[] dates = parsePeriod(period);

        // 가장 최근 클러스터 조회
        IssueCluster cluster = issueClusterRepository
                .findTopBySearchKeywordAndPeriodStartDateAndPeriodEndDateOrderByCreatedAtDesc(
                        searchKeyword, dates[0], dates[1])
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "해당 키워드·기간의 비교 검색 결과가 없습니다. 먼저 /search를 호출하세요."));

        // 국가별 대표 영상 + bias 조회
        List<IssueComparisonResponseDto.CountryResult> results = new ArrayList<>();
        for (String countryCode : countryList) {
            // 대표 영상 우선, 없으면 첫 번째 영상
            IssueClusterVideo icv = issueClusterVideoRepository
                    .findFirstByIssueClusterAndCountryCodeAndIsRepresentativeTrue(cluster, countryCode)
                    .or(() -> issueClusterVideoRepository.findFirstByIssueClusterAndCountryCode(cluster, countryCode))
                    .orElse(null);

            if (icv == null) continue;

            // bias_analysis_result 조회 (없어도 null로 처리)
            BiasAnalysisResult bias = biasAnalysisResultRepository
                    .findByTargetIdAndTargetType(icv.getYoutubeVideo().getId(), TargetType.YOUTUBE_VIDEO)
                    .orElse(null);

            results.add(IssueConverter.toComparisonCountryResult(countryCode, icv, bias, searchKeyword));
        }

        return IssueComparisonResponseDto.builder()
                .searchKeyword(searchKeyword)
                .countries(results)
                .build();
    }

    // 국가별 이슈 영상 편향 조회
    @Transactional(readOnly = true)
    public IssueBiasResponseDto bias(IssueBiasRequestDto request) {
        List<IssueBiasResponseDto.BiasResult> results = new ArrayList<>();

        for (String youtubeVideoId : request.getYoutubeVideoIds()) {
            youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId).ifPresent(video -> {
                biasAnalysisResultRepository
                        .findByTargetIdAndTargetType(video.getId(), TargetType.YOUTUBE_VIDEO)
                        .ifPresent(bias -> results.add(
                                IssueConverter.toBiasResult(youtubeVideoId, video.getCountryCode(), bias)));
            });
        }

        return IssueBiasResponseDto.builder().results(results).build();
    }

    // 인기 비교 이슈 영상 카드
    @Transactional(readOnly = true)
    public IssuePopularVideosResponseDto popularVideos(int size) {
        List<IssueClusterVideo> topVideos = issueClusterVideoRepository
                .findTopByViewCountDesc(PageRequest.of(0, size));

        List<IssuePopularVideosResponseDto.VideoCard> cards = topVideos.stream()
                .map(IssueConverter::toPopularVideoCard)
                .toList();

        return IssuePopularVideosResponseDto.builder().videos(cards).build();
    }

    // 유틸 기능들
    private LocalDate[] parsePeriod(String period) {
        int days = "7d".equals(period) ? 7 : 30;
        LocalDate end = LocalDate.now();
        return new LocalDate[]{end.minusDays(days), end};
    }

    private List<String> parseCountries(String countries) {
        return Arrays.asList(countries.split(","));
    }

    private void saveClusterVideos(IssueCluster cluster, String countryCode,
                                   List<com.example.news.domain.content.dto.YoutubeVideoDto.VideoCard> videoCards) {
        for (var card : videoCards) {
            youtubeVideoRepository.findByYoutubeVideoId(card.getYoutubeVideoId())
                    .ifPresent(video -> issueClusterVideoRepository.save(
                            IssueClusterVideo.builder()
                                    .issueCluster(cluster)
                                    .youtubeVideo(video)
                                    .countryCode(countryCode)
                                    .isRepresentative(false)
                                    .build()
                    ));
        }
    }
}
