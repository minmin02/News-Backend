package com.example.news.domain.issue.converter;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.entity.ComparisonCountryItem;
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
                .issueClusterId(cluster.getId())
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
            ComparisonCountryItem countryItem,
            YoutubeVideo video,
            BiasAnalysisResult analysisResult) {

        String countryCode = countryItem.getCountryCode();
        CountryCode country = CountryCode.of(countryCode);

        String summaryText = countryItem.getSummaryText();
        String toneLabel = analysisResult != null ? analysisResult.getToneLabel() : null;
        Double overallBiasScore = analysisResult != null && analysisResult.getOverallBiasScore() != null
                ? analysisResult.getOverallBiasScore()
                : countryItem.getBiasScore();
        Double opinionScore = analysisResult != null ? analysisResult.getOpinionScore() : null;
        Double emotionScore = analysisResult != null ? analysisResult.getEmotionScore() : null;

        IssueComparisonResponseDto.ComparisonSummary comparison =
                IssueComparisonResponseDto.ComparisonSummary.builder()
                        .perspectiveSummary(summaryText)
                        .toneLabel(toneLabel)
                        .representativeChannelName(video != null ? video.getChannelName() : null)
                        .coreKeywords(List.of())
                        .build();

        return IssueComparisonResponseDto.CountryResult.builder()
                .countryCode(countryCode)
                .countryName(country.getCountryName())
                .languageLabel(country.getLanguageLabel())
                .videoId(video != null ? video.getId() : countryItem.getRepresentativeVideoId())
                .youtubeVideoId(video != null ? video.getYoutubeVideoId() : null)
                .title(countryItem.getTitle() != null ? countryItem.getTitle() : (video != null ? video.getTitle() : null))
                .channelName(video != null ? video.getChannelName() : null)
                .thumbnailUrl(video != null ? video.getThumbnailUrl() : null)
                .originalUrl(video != null ? video.getOriginalUrl() : null)
                .publishedAt(video != null ? video.getPublishedAt() : null)
                .overallBiasScore(overallBiasScore)
                .opinionScore(opinionScore)
                .emotionScore(emotionScore)
                .toneLabel(toneLabel)
                .perspectiveSummary(summaryText)
                .evidenceSummary(null)
                .comparison(comparison)
                .build();
    }
}
