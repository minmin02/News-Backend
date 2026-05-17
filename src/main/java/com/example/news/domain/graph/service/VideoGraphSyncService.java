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

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGraphSyncService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final Map<String, String> DEFAULT_LANGUAGE_BY_COUNTRY = Map.of(
            "KR", "ko",
            "US", "en",
            "CN", "zh"
    );

    private final VideoNodeRepository videoNodeRepository;
    private final ChannelNodeRepository channelNodeRepository;

    @Async
    public void syncVideo(YoutubeVideo video) {
        syncVideoNow(video);
    }

    public void syncVideoNow(YoutubeVideo video) {
        syncVideoNow(video, null);
    }

    public void syncVideoNow(YoutubeVideo video, String countryCodeHint) {
        try {
            String normalizedCountryCode = normalizeCountryCode(video.getCountryCode());
            String fallbackCountryCode = normalizeCountryCode(countryCodeHint);
            String finalCountryCode = normalizedCountryCode != null ? normalizedCountryCode : fallbackCountryCode;
            String language = normalizeLanguage(video.getDefaultLanguageCode(), finalCountryCode);
            if (finalCountryCode == null || language == null) {
                logPipelineFailure(
                        "video",
                        video.getYoutubeVideoId(),
                        video.getId(),
                        null,
                        "missing required country_code/language after normalization",
                        false
                );
                return;
            }

            // Channel 노드 upsert
            ChannelNode channel = channelNodeRepository.findNodeOnlyByChannelId(video.getChannelId())
                    .orElseGet(() -> ChannelNode.builder()
                            .channelId(video.getChannelId())
                            .channelName(video.getChannelName())
                            .countryCode(finalCountryCode)
                            .build());
            channelNodeRepository.save(channel);

            // Video 노드 load-then-update — 기존 PART_OF 관계 보존
            VideoNode videoNode = videoNodeRepository.findNodeOnlyByVideoId(video.getYoutubeVideoId())
                    .orElseGet(() -> VideoNode.builder()
                            .youtubeVideoId(video.getYoutubeVideoId())
                            .build());

            videoNode.setTargetId(video.getId());           // PostgreSQL PK → /target 엔드포인트에서 사용
            videoNode.setTitle(video.getTitle());
            videoNode.setDescription(video.getDescription());
            videoNode.setCountryCode(finalCountryCode);
            videoNode.setLanguageCode(language);
            videoNode.setChannelId(video.getChannelId());
            videoNode.setPublishedAt(video.getPublishedAt());
            videoNode.setViewCount(video.getViewCount());
            videoNode.setStatus(ACTIVE_STATUS);
            videoNode.setThumbnailUrl(video.getThumbnailUrl());
            videoNode.setChannel(channel);

            videoNodeRepository.save(videoNode);

        } catch (Exception e) {
            logPipelineFailure("video", video.getYoutubeVideoId(), video.getId(), null, e.getMessage(), true);
            log.warn("[GraphSync] Video 동기화 예외", e);
        }
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return null;
        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        return DEFAULT_LANGUAGE_BY_COUNTRY.containsKey(normalized) ? normalized : null;
    }

    private String normalizeLanguage(String language, String normalizedCountryCode) {
        if (language != null && !language.isBlank()) {
            String trimmed = language.trim().toLowerCase(Locale.ROOT);
            if (trimmed.length() >= 2) {
                return trimmed.substring(0, 2);
            }
        }
        return normalizedCountryCode == null ? null : DEFAULT_LANGUAGE_BY_COUNTRY.get(normalizedCountryCode);
    }

    private void logPipelineFailure(String pipeline,
                                    String videoId,
                                    Long targetId,
                                    Long issueId,
                                    String reason,
                                    boolean hasStackTrace) {
        log.warn(
                "pipeline={} video_id={} target_id={} issue_id={} reason=\"{}\" stacktrace={} event_time={}",
                pipeline,
                videoId,
                targetId,
                issueId,
                reason,
                hasStackTrace,
                OffsetDateTime.now()
        );
    }
}
