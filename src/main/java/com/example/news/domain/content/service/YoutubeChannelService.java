package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.entity.ContentSource;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.domain.content.repository.ContentSourceRepository;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.global.exception.CustomException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeChannelService {

    private final YouTube youtubeClient;
    private final ContentSourceRepository contentSourceRepository;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final YoutubeSearchService youtubeSearchService;

    @Value("${youtube.api.key}")
    private String apiKey;

    // 추천 채널 영상 조회
    @Transactional
    public Map<String, List<YoutubeVideoDto.VideoCard>> getChannelVideos() {
        List<ContentSource> featuredChannels = contentSourceRepository.findByIsFeaturedTrue();
        Map<String, List<YoutubeVideoDto.VideoCard>> result = new LinkedHashMap<>();

        for (ContentSource channel : featuredChannels) {
            if (channel.getYoutubeChannelId() == null) continue;
            List<YoutubeVideoDto.VideoCard> videos = fetchChannelVideos(channel);
            result.put(channel.getDisplayName() != null ? channel.getDisplayName() : channel.getName(), videos);
        }

        return result;
    }

    // 채널 별 영상 조회
    private List<YoutubeVideoDto.VideoCard> fetchChannelVideos(ContentSource channel) {
        // DB에 저장된 영상 먼저 확인
        List<YoutubeVideo> cached = youtubeVideoRepository
                .findByContentSourceOrderByPublishedAtDesc(channel);
        if (!cached.isEmpty()) {
            return cached.stream()
                    .limit(5)
                    .map(YoutubeConverter::toVideoCard)
                    .collect(Collectors.toList());
        }

        // YouTube API로 채널 최신 영상 조회
        try {
            YouTube.Search.List searchRequest = youtubeClient.search().list(List.of("snippet"));
            searchRequest.setKey(apiKey);
            searchRequest.setChannelId(channel.getYoutubeChannelId());
            searchRequest.setType(List.of("video"));
            searchRequest.setOrder("date");
            searchRequest.setMaxResults(5L);

            SearchListResponse response = searchRequest.execute();
            if (response.getItems() == null || response.getItems().isEmpty()) {
                return List.of();
            }

            List<String> videoIds = response.getItems().stream()
                    .map(item -> item.getId().getVideoId())
                    .collect(Collectors.toList());

            YouTube.Videos.List videosRequest = youtubeClient.videos()
                    .list(List.of("snippet", "contentDetails", "statistics"));
            videosRequest.setKey(apiKey);
            videosRequest.setId(videoIds);

            VideoListResponse videosResponse = videosRequest.execute();
            return videosResponse.getItems().stream()
                    .map(video -> {
                        YoutubeVideo saved = saveChannelVideo(video, channel);
                        return YoutubeConverter.toVideoCard(saved);
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.warn("Failed to fetch videos for channel {}: {}", channel.getYoutubeChannelId(), e.getMessage());
            return List.of();
        }
    }

    // 채널 영상 저장
    private YoutubeVideo saveChannelVideo(com.google.api.services.youtube.model.Video video, ContentSource channel) {
        return youtubeVideoRepository.findByYoutubeVideoId(video.getId())
                .orElseGet(() -> {
                    YoutubeVideo saved = youtubeSearchService.saveVideo(video);
                    // contentSource 연결은 별도 처리 필요 (현재 saveVideo가 contentSource를 null로 저장)
                    return saved;
                });
    }
}
