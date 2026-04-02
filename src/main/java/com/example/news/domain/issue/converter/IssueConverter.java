package com.example.news.domain.issue.converter;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterItem;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IssueConverter {

    private static final Map<String, String> COUNTRY_NAMES = Map.of(
            "KR", "한국", "US", "미국", "CN", "중국", "JP", "일본"
    );

    private static final Map<String, String> LANGUAGE_LABELS = Map.of(
            "KR", "한국어", "US", "영어", "CN", "중국어", "JP", "일본어"
    );

    private static final Map<String, String> COUNTRY_TO_LANG = Map.of(
            "KR", "ko", "US", "en", "CN", "zh-CN", "JP", "ja"
    );

    // 국가코드 => youtube api용 언어코드 반환 ex KR => ko
    public static String getLanguageCode(String countryCode) {
        String lang = COUNTRY_TO_LANG.get(countryCode);
        if (lang == null) throw new IllegalArgumentException("Unsupported country code: " + countryCode);
        return lang;
    }

    // KR, JP, US => 리스트로 변환
    public static List<String> parseCountries(String countries) {
        return Arrays.asList(countries.split(","));
    }

    // 7d => 오늘 ~ 7일로 변환
    public static LocalDate[] parsePeriod(String period) {
        int days = "7d".equals(period) ? 7 : 30;
        LocalDate end = LocalDate.now();
        return new LocalDate[]{end.minusDays(days), end};
    }

    // 국가별 이슈 영상 검색
    public static IssueSearchResponseDto.VideoResult toVideoResult(IssueClusterItem item, YoutubeVideo video) {
        return IssueSearchResponseDto.VideoResult.builder()
                .videoId(video.getId())
                .youtubeVideoId(video.getYoutubeVideoId())
                .countryCode(item.getCountryCode())
                .title(video.getTitle())
                .channelName(video.getChannelName())
                .thumbnailUrl(video.getThumbnailUrl())
                .publishedAt(video.getPublishedAt())
                .similarityScore(item.getSimilarityScore())
                .isRepresentative(item.getIsRepresentative())
                .build();
    }

    public static IssueSearchResponseDto toSearchResponse(IssueCluster cluster,
                                                          List<IssueClusterItem> items,
                                                          Map<Long, YoutubeVideo> videoMap) {
        return IssueSearchResponseDto.builder()
                .searchKeyword(cluster.getSearchKeyword())
                .clusterTitle(cluster.getClusterTitle())
                .clusterSummary(cluster.getClusterSummary())
                .periodStartDate(cluster.getPeriodStartDate())
                .periodEndDate(cluster.getPeriodEndDate())
                .results(items.stream()
                        .filter(item -> videoMap.containsKey(item.getYoutubeVideoId()))
                        .map(item -> toVideoResult(item, videoMap.get(item.getYoutubeVideoId())))
                        .toList())
                .build();
    }

    public static IssueComparisonResponseDto.CountryResult toComparisonCountryResult(
            String countryCode,
            YoutubeVideo video,
            BiasAnalysisResult bias) {

        IssueComparisonResponseDto.ComparisonSummary comparison =
                IssueComparisonResponseDto.ComparisonSummary.builder()
                        .perspectiveSummary(bias != null ? bias.getPerspectiveSummary() : null)
                        .toneLabel(bias != null ? bias.getToneLabel() : null)
                        .representativeChannelName(video.getChannelName())
                        .coreKeywords(List.of())
                        .build();

        return IssueComparisonResponseDto.CountryResult.builder()
                .countryCode(countryCode)
                .countryName(COUNTRY_NAMES.getOrDefault(countryCode, countryCode))
                .languageLabel(LANGUAGE_LABELS.getOrDefault(countryCode, countryCode))
                .videoId(video.getId())
                .youtubeVideoId(video.getYoutubeVideoId())
                .title(video.getTitle())
                .channelName(video.getChannelName())
                .thumbnailUrl(video.getThumbnailUrl())
                .originalUrl(video.getOriginalUrl())
                .publishedAt(video.getPublishedAt())
                .overallBiasScore(bias != null ? bias.getOverallBiasScore() : null)
                .opinionScore(bias != null ? bias.getOpinionScore() : null)
                .emotionScore(bias != null ? bias.getEmotionScore() : null)
                .toneLabel(bias != null ? bias.getToneLabel() : null)
                .perspectiveSummary(bias != null ? bias.getPerspectiveSummary() : null)
                .evidenceSummary(bias != null ? bias.getEvidenceSummary() : null)
                .comparison(comparison)
                .build();
    }
}
