package com.example.news.domain.issue.converter;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterVideo;

import java.util.List;
import java.util.Map;

public class IssueConverter {

    private static final Map<String, String> COUNTRY_NAMES = Map.of(
            "KR", "한국", "US", "미국", "CN", "중국", "JP", "일본"
    );

    private static final Map<String, String> LANGUAGE_LABELS = Map.of(
            "KR", "한국어", "US", "영어", "CN", "중국어", "JP", "일본어"
    );

    // 국가별 이슈 영상 검색

    public static IssueSearchResponseDto.VideoResult toVideoResult(IssueClusterVideo icv) {
        return IssueSearchResponseDto.VideoResult.builder()
                .videoId(icv.getYoutubeVideo().getId())
                .youtubeVideoId(icv.getYoutubeVideo().getYoutubeVideoId())
                .countryCode(icv.getCountryCode())
                .title(icv.getYoutubeVideo().getTitle())
                .channelName(icv.getYoutubeVideo().getChannelName())
                .thumbnailUrl(icv.getYoutubeVideo().getThumbnailUrl())
                .publishedAt(icv.getYoutubeVideo().getPublishedAt())
                .similarityScore(icv.getSimilarityScore())
                .isRepresentative(icv.getIsRepresentative())
                .build();
    }

    public static IssueSearchResponseDto toSearchResponse(IssueCluster cluster, List<IssueClusterVideo> videos) {
        return IssueSearchResponseDto.builder()
                .searchKeyword(cluster.getSearchKeyword())
                .clusterTitle(cluster.getClusterTitle())
                .clusterSummary(cluster.getClusterSummary())
                .periodStartDate(cluster.getPeriodStartDate())
                .periodEndDate(cluster.getPeriodEndDate())
                .results(videos.stream().map(IssueConverter::toVideoResult).toList())
                .build();
    }

    // 국가별 대표 영상 비교 결과 조회

    public static IssueComparisonResponseDto.CountryResult toComparisonCountryResult(
            String countryCode,
            IssueClusterVideo icv,
            BiasAnalysisResult bias,
            String searchKeyword) {

        var video = icv.getYoutubeVideo();

        IssueComparisonResponseDto.ComparisonSummary comparison =
                IssueComparisonResponseDto.ComparisonSummary.builder()
                        .searchKeyword(searchKeyword)
                        .perspectiveSummary(bias != null ? bias.getPerspectiveSummary() : null)
                        .toneLabel(bias != null ? bias.getToneLabel() : null)
                        .representativeChannelName(video.getChannelName())
                        .coreKeywords(List.of())  // BiasAnalysisKeyword는 별도 조회 없이 빈 배열
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

    // 국가별 이슈 영상 편향 조회

    public static IssueBiasResponseDto.BiasResult toBiasResult(String youtubeVideoId, String countryCode, BiasAnalysisResult bias) {
        return IssueBiasResponseDto.BiasResult.builder()
                .countryCode(countryCode)
                .youtubeVideoId(youtubeVideoId)
                .overallBiasScore(bias.getOverallBiasScore())
                .opinionScore(bias.getOpinionScore())
                .emotionScore(bias.getEmotionScore())
                .anonymousSourceScore(bias.getAnonymousSourceScore())
                .headlineBodyGapScore(bias.getHeadlineBodyGapScore())
                .neutralityScore(bias.getNeutralityScore())
                .toneLabel(bias.getToneLabel())
                .perspectiveSummary(bias.getPerspectiveSummary())
                .build();
    }

    // 인기 비교 이슈 영상 카드

    public static IssuePopularVideosResponseDto.VideoCard toPopularVideoCard(IssueClusterVideo icv) {
        var video = icv.getYoutubeVideo();
        return IssuePopularVideosResponseDto.VideoCard.builder()
                .videoId(video.getId())
                .youtubeVideoId(video.getYoutubeVideoId())
                .title(video.getTitle())
                .channelName(video.getChannelName())
                .thumbnailUrl(video.getThumbnailUrl())
                .originalUrl(video.getOriginalUrl())
                .publishedAt(video.getPublishedAt())
                .viewCount(video.getViewCount())
                .durationSeconds(video.getDurationSeconds())
                .countryCode(icv.getCountryCode())
                .build();
    }
}
