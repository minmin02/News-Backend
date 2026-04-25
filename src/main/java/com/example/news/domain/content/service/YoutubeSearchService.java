package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.entity.Keyword;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.entity.YoutubeVideoKeyword;
import com.example.news.domain.content.exception.YoutubeApiException;
import com.example.news.domain.content.repository.KeywordRepository;
import com.example.news.domain.content.repository.YoutubeVideoKeywordRepository;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeSearchService {

    // 키워드 검색 핵심 서비스
    private final YouTube youtubeClient;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final KeywordRepository keywordRepository;
    private final YoutubeVideoKeywordRepository youtubeVideoKeywordRepository;
    private final TitleTranslationService titleTranslationService;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Transactional
    public List<YoutubeVideoDto.VideoCard> search(String keyword) {
        List<String> videoIds = searchVideoIds(keyword);
        if (videoIds.isEmpty()) {
            return List.of();
        }

        List<YoutubeVideo> videos = fetchAndSaveVideos(videoIds);
        translateTitlesIfNeeded(videos);
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
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new YoutubeApiException(e.getMessage(), e);
        }
    }

    // 국가별 비교용 검색 (번역된 키워드 + 지역/언어 + 날짜 범위)
    @Transactional
    public List<YoutubeVideoDto.VideoCard> searchByRegion(
            String keyword,
            String regionCode,
            String relevanceLanguage,
            LocalDate startDate,
            LocalDate endDate) {

        List<String> videoIds = searchVideoIdsByRegion(keyword, regionCode, relevanceLanguage, startDate, endDate);
        if (videoIds.isEmpty()) {
            return List.of();
        }

        List<YoutubeVideo> videos = fetchAndSaveVideos(videoIds);
        translateTitlesIfNeeded(videos);
        linkKeywordToVideos(keyword, videos);

        return videos.stream()
                .map(YoutubeConverter::toVideoCard)
                .collect(Collectors.toList());
    }

    private List<String> searchVideoIdsByRegion(
            String keyword,
            String regionCode,
            String relevanceLanguage,
            LocalDate startDate,
            LocalDate endDate) {
        try {
            YouTube.Search.List searchRequest = youtubeClient.search().list(List.of("snippet"));
            searchRequest.setKey(apiKey);
            searchRequest.setQ(keyword);
            searchRequest.setType(List.of("video"));
            searchRequest.setMaxResults(20L);
            searchRequest.setRegionCode(regionCode);
            searchRequest.setRelevanceLanguage(relevanceLanguage);

            if (startDate != null) {
                searchRequest.setPublishedAfter(
                        startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toString());
            }
            if (endDate != null) {
                searchRequest.setPublishedBefore(
                        endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString());
            }

            SearchListResponse response = searchRequest.execute();
            return response.getItems().stream()
                    .map(item -> item.getId().getVideoId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new YoutubeApiException(e.getMessage(), e);
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
            throw new YoutubeApiException(e.getMessage(), e);
        }

        return result;
    }

    // defaultLanguageCode가 한국어가 아닌 영상 제목만 번역해서 DB 업데이트
    private void translateTitlesIfNeeded(List<YoutubeVideo> videos) {
        for (YoutubeVideo video : videos) {
            String lang = video.getDefaultLanguageCode();
            if (lang != null && !lang.startsWith("ko")) {
                String translated = titleTranslationService.translateToKorean(video.getTitle());
                video.updateTitle(translated);
            }
        }
    }

    // 영상 단건 저장
    YoutubeVideo saveVideo(Video video) {
        String videoId = video.getId();
        return youtubeVideoRepository.findByYoutubeVideoId(videoId)
                .orElseGet(() -> youtubeVideoRepository.save(YoutubeConverter.toYoutubeVideoEntity(video)));
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

}
