package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.entity.ContentSource;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.SourceType;
import com.example.news.domain.content.repository.ContentSourceRepository;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeChannelService {

    private final YouTube youtubeClient;
    private final ContentSourceRepository contentSourceRepository;
    private final YoutubeVideoRepository youtubeVideoRepository;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("#{'${youtube.featured-channel-ids}'.split(',')}")
    private List<String> featuredChannelIds;

    // 추천 채널 영상 조회
    @Transactional
    public Map<String, List<YoutubeVideoDto.VideoCard>> getChannelVideos() {
        List<ContentSource> featuredChannels = getOrInitFeaturedChannels();
        Map<String, List<YoutubeVideoDto.VideoCard>> result = new LinkedHashMap<>();

        for (ContentSource channel : featuredChannels) {
            List<YoutubeVideoDto.VideoCard> videos = fetchChannelVideos(channel);
            result.put(channel.getDisplayName() != null ? channel.getDisplayName() : channel.getName(), videos);
        }

        return result;
    }

    // yml의 채널 ID 중 DB에 없는 것만 YouTube API로 추가 생성 후 전체 반환
    private List<ContentSource> getOrInitFeaturedChannels() {
        // DB에 없는 채널 ID만 추출
        List<String> missingIds = featuredChannelIds.stream()
                .filter(id -> contentSourceRepository.findByYoutubeChannelId(id).isEmpty())
                .collect(Collectors.toList());

        // 누락된 채널이 있으면 YouTube API로 일괄 조회 후 저장
        if (!missingIds.isEmpty()) {
            try {
                YouTube.Channels.List request = youtubeClient.channels()
                        .list(List.of("snippet", "brandingSettings"));
                request.setKey(apiKey);
                request.setId(missingIds);

                ChannelListResponse response = request.execute();
                if (response.getItems() != null) {
                    response.getItems().forEach(this::getOrCreateContentSource);
                }
            } catch (IOException e) {
                log.warn("Failed to initialize featured channels: {}", e.getMessage());
            }
        }

        return contentSourceRepository.findByIsFeaturedTrue();
    }

    // YouTube Channel → ContentSource 생성 (없을 때만)
    private ContentSource getOrCreateContentSource(Channel channel) {
        return contentSourceRepository.findByYoutubeChannelId(channel.getId())
                .orElseGet(() -> contentSourceRepository.save(
                        ContentSource.builder()
                                .name(channel.getSnippet().getTitle())
                                .displayName(channel.getSnippet().getTitle())
                                .sourceType(SourceType.BROADCAST)
                                .countryCode(channel.getSnippet().getCountry())
                                .youtubeChannelId(channel.getId())
                                .youtubeChannelUrl("https://www.youtube.com/channel/" + channel.getId())
                                .isFeatured(true)
                                .logoUrl(extractChannelThumbnail(channel))
                                .build()
                ));
    }

    // 채널별 최신 영상 조회 (DB 캐시 우선)
    private List<YoutubeVideoDto.VideoCard> fetchChannelVideos(ContentSource channel) {
        List<YoutubeVideo> cached = youtubeVideoRepository
                .findByContentSourceOrderByPublishedAtDesc(channel);
        if (!cached.isEmpty()) {
            return cached.stream()
                    .map(YoutubeConverter::toVideoCard)
                    .collect(Collectors.toList());
        }

        // YouTube search API로 채널 최신 영상 5개 조회
        try {
            YouTube.Search.List searchRequest = youtubeClient.search().list(List.of("snippet"));
            searchRequest.setKey(apiKey);
            searchRequest.setChannelId(channel.getYoutubeChannelId());
            searchRequest.setType(List.of("video"));
            searchRequest.setOrder("date");
            searchRequest.setMaxResults(25L);

            SearchListResponse searchResponse = searchRequest.execute();
            if (searchResponse.getItems() == null || searchResponse.getItems().isEmpty()) {
                return List.of();
            }

            List<String> videoIds = searchResponse.getItems().stream()
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

    // 채널 영상 저장 (contentSource 포함)
    private YoutubeVideo saveChannelVideo(Video video, ContentSource channel) {
        return youtubeVideoRepository.findByYoutubeVideoId(video.getId())
                .orElseGet(() -> youtubeVideoRepository.save(
                        YoutubeConverter.toYoutubeVideoEntity(video, channel)
                ));
    }

    private String extractChannelThumbnail(Channel channel) {
        try {
            return channel.getSnippet().getThumbnails().getHigh().getUrl();
        } catch (NullPointerException e) {
            try {
                return channel.getSnippet().getThumbnails().getDefault().getUrl();
            } catch (NullPointerException ex) {
                return null;
            }
        }
    }
}
