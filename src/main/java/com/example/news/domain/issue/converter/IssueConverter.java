package com.example.news.domain.issue.converter;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterItem;
import com.example.news.domain.issue.enums.CountryCode;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IssueConverter {

    // 국가코드 => youtube api용 언어코드 반환 ex KR => ko
    public static String getLanguageCode(String countryCode) {
        return CountryCode.of(countryCode).getLanguageCode();
    }

    // KR, JP, US => 리스트로 변환
    public static List<String> parseCountries(String countries) {
        return Arrays.asList(countries.split(","));
    }

    // days => 오늘 ~ N일로 변환
    public static LocalDate[] parsePeriod(int days) {
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

        CountryCode country = CountryCode.of(countryCode);

        IssueComparisonResponseDto.ComparisonSummary comparison =
                IssueComparisonResponseDto.ComparisonSummary.builder()
                        .perspectiveSummary(bias != null ? bias.getPerspectiveSummary() : null)
                        .toneLabel(bias != null ? bias.getToneLabel() : null)
                        .representativeChannelName(video.getChannelName())
                        .coreKeywords(List.of())
                        .build();

        return IssueComparisonResponseDto.CountryResult.builder()
                .countryCode(countryCode)
                .countryName(country.getCountryName())
                .languageLabel(country.getLanguageLabel())
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
