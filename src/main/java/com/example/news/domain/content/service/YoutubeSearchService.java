package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.entity.Keyword;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.entity.YoutubeVideoKeyword;
import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.domain.content.repository.KeywordRepository;
import com.example.news.domain.content.repository.YoutubeVideoKeywordRepository;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.global.exception.CustomException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YoutubeSearchService {

    // 키워드 검색 핵심 서비스
    private final YouTube youtubeClient;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final KeywordRepository keywordRepository;
    private final YoutubeVideoKeywordRepository youtubeVideoKeywordRepository;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Transactional
    public List<YoutubeVideoDto.VideoCard> search(String keyword) {
        List<String> videoIds = searchVideoIds(keyword);
        if (videoIds.isEmpty()) {
            return List.of();
        }

        List<YoutubeVideo> videos = fetchAndSaveVideos(videoIds);
        linkKeywordToVideos(keyword, videos);

        return videos.stream()
                .map(YoutubeConverter::toVideoCard)
                .collect(Collectors.toList());
    }

    // 유튜브 검색 api 호출
    private List<String> searchVideoIds(String keyword) {
        try {
            YouTube.Search.List searchRequest = youtubeClient.search().list(List.of("snippet"));
            searchRequest.setKey(apiKey);
            searchRequest.setQ(keyword);
            searchRequest.setType(List.of("video"));
            searchRequest.setMaxResults(20L);
            searchRequest.setRelevanceLanguage("ko");

            SearchListResponse response = searchRequest.execute();
            return response.getItems().stream()
                    .map(item -> item.getId().getVideoId())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new CustomException(YoutubeErrorCode.YOUTUBE_API_ERROR, e.getMessage(), e);
        }
    }

    private List<YoutubeVideo> fetchAndSaveVideos(List<String> videoIds) {
        List<YoutubeVideo> result = new ArrayList<>();

        // DB에 이미 있는 영상은 재호출 없이 반환
        List<String> missingIds = new ArrayList<>();
        for (String videoId : videoIds) {
            youtubeVideoRepository.findByYoutubeVideoId(videoId).ifPresentOrElse(
                    result::add,
                    () -> missingIds.add(videoId)
            );
        }

        if (missingIds.isEmpty()) return result;

        // 없는 것만 YouTube API로 가져오기
        try {
            YouTube.Videos.List videosRequest = youtubeClient.videos()
                    .list(List.of("snippet", "contentDetails", "statistics"));
            videosRequest.setKey(apiKey);
            videosRequest.setId(missingIds);

            VideoListResponse response = videosRequest.execute();
            for (Video video : response.getItems()) {
                YoutubeVideo saved = saveVideo(video);
                result.add(saved);
            }
        } catch (IOException e) {
            throw new CustomException(YoutubeErrorCode.YOUTUBE_API_ERROR, e.getMessage(), e);
        }

        return result;
    }

    // 영상 단건 저장
    YoutubeVideo saveVideo(Video video) {
        VideoSnippet snippet = video.getSnippet();
        VideoContentDetails contentDetails = video.getContentDetails();
        VideoStatistics statistics = video.getStatistics();

        String videoId = video.getId();
        return youtubeVideoRepository.findByYoutubeVideoId(videoId).orElseGet(() -> {
            YoutubeVideo entity = YoutubeVideo.builder()
                    .youtubeVideoId(videoId)
                    .originalUrl("https://www.youtube.com/watch?v=" + videoId)
                    .channelId(snippet.getChannelId())
                    .channelName(snippet.getChannelTitle())
                    .title(snippet.getTitle())
                    .description(snippet.getDescription())
                    .thumbnailUrl(extractThumbnailUrl(snippet))
                    .publishedAt(parseDateTime(snippet.getPublishedAt()))
                    .collectedAt(LocalDateTime.now())
                    .defaultLanguageCode(snippet.getDefaultLanguage() != null
                            ? snippet.getDefaultLanguage()
                            : snippet.getDefaultAudioLanguage())
                    .durationSeconds(contentDetails != null
                            ? parseDuration(contentDetails.getDuration()) : null)
                    .isEmbeddable(contentDetails != null
                            ? contentDetails.getLicensedContent() : null)
                    .viewCount(statistics != null && statistics.getViewCount() != null
                            ? statistics.getViewCount().longValue() : null)
                    .likeCount(statistics != null && statistics.getLikeCount() != null
                            ? statistics.getLikeCount().longValue() : null)
                    .commentCount(statistics != null && statistics.getCommentCount() != null
                            ? statistics.getCommentCount().longValue() : null)
                    .build();
            return youtubeVideoRepository.save(entity);
        });
    }

    // 영상-키워드 연결 중복 없이 저장
    private void linkKeywordToVideos(String keyword, List<YoutubeVideo> videos) {
        String normalized = keyword.trim().toLowerCase();
        Keyword keywordEntity = keywordRepository.findByNormalizedKeyword(normalized)
                .orElseGet(() -> keywordRepository.save(
                        Keyword.builder()
                                .keywordName(keyword)
                                .normalizedKeyword(normalized)
                                .build()
                ));

        for (YoutubeVideo video : videos) {
            if (!youtubeVideoKeywordRepository.existsByYoutubeVideoAndKeyword(video, keywordEntity)) {
                youtubeVideoKeywordRepository.save(
                        YoutubeVideoKeyword.builder()
                                .youtubeVideo(video)
                                .keyword(keywordEntity)
                                .build()
                );
            }
        }
    }

    // 썸네일 url 추출
    private String extractThumbnailUrl(VideoSnippet snippet) {
        if (snippet.getThumbnails() == null) return null;
        if (snippet.getThumbnails().getHigh() != null) return snippet.getThumbnails().getHigh().getUrl();
        if (snippet.getThumbnails().getMedium() != null) return snippet.getThumbnails().getMedium().getUrl();
        if (snippet.getThumbnails().getDefault() != null) return snippet.getThumbnails().getDefault().getUrl();
        return null;
    }

    // 날짜 파싱
    private LocalDateTime parseDateTime(com.google.api.client.util.DateTime dateTime) {
        if (dateTime == null) return null;
        return ZonedDateTime.parse(dateTime.toStringRfc3339()).toLocalDateTime();
    }

    // 영상 길이 파싱
    private Integer parseDuration(String isoDuration) {
        if (isoDuration == null) return null;
        try {
            return (int) Duration.parse(isoDuration).getSeconds();
        } catch (Exception e) {
            return null;
        }
    }
}
