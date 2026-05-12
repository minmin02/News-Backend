package com.example.news.domain.graph.service;

import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.graph.node.ChannelNode;
import com.example.news.domain.graph.node.VideoNode;
import com.example.news.domain.graph.repository.ChannelNodeRepository;
import com.example.news.domain.graph.repository.VideoNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGraphSyncService {

    private final VideoNodeRepository videoNodeRepository;
    private final ChannelNodeRepository channelNodeRepository;

    @Async
    public void syncVideo(YoutubeVideo video) {
        try {
            // Channel 노드 upsert
            ChannelNode channel = channelNodeRepository.findById(video.getChannelId())
                    .orElseGet(() -> ChannelNode.builder()
                            .channelId(video.getChannelId())
                            .channelName(video.getChannelName())
                            .countryCode(video.getCountryCode())
                            .build());
            channelNodeRepository.save(channel);

            // Video 노드 load-then-update — 기존 PART_OF 관계 보존
            VideoNode videoNode = videoNodeRepository.findById(video.getYoutubeVideoId())
                    .orElseGet(() -> VideoNode.builder()
                            .youtubeVideoId(video.getYoutubeVideoId())
                            .build());

            videoNode.setTargetId(video.getId());           // PostgreSQL PK → /target 엔드포인트에서 사용
            videoNode.setTitle(video.getTitle());
            videoNode.setDescription(video.getDescription());
            videoNode.setCountryCode(video.getCountryCode());
            videoNode.setLanguageCode(video.getDefaultLanguageCode());
            videoNode.setChannelId(video.getChannelId());
            videoNode.setPublishedAt(video.getPublishedAt());
            videoNode.setViewCount(video.getViewCount());
            videoNode.setThumbnailUrl(video.getThumbnailUrl());
            videoNode.setChannel(channel);

            videoNodeRepository.save(videoNode);

        } catch (Exception e) {
            log.warn("[GraphSync] Video 동기화 실패 - videoId={}, error={}",
                    video.getYoutubeVideoId(), e.getMessage());
        }
    }
}
