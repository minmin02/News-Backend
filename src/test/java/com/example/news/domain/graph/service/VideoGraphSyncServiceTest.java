package com.example.news.domain.graph.service;

import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.graph.node.ChannelNode;
import com.example.news.domain.graph.node.VideoNode;
import com.example.news.domain.graph.repository.ChannelNodeRepository;
import com.example.news.domain.graph.repository.VideoNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoGraphSyncServiceTest {

    @Mock
    VideoNodeRepository videoNodeRepository;

    @Mock
    ChannelNodeRepository channelNodeRepository;

    @InjectMocks
    VideoGraphSyncService videoGraphSyncService;

    @Test
    void syncVideo_savesVideoAndChannel_withNormalizedLanguageAndStatus() {
        YoutubeVideo video = YoutubeVideo.builder()
                .id(10L)
                .youtubeVideoId("yt-1")
                .channelId("ch-1")
                .channelName("channel")
                .countryCode("kr")
                .defaultLanguageCode(null)
                .title("title")
                .description("desc")
                .publishedAt(LocalDateTime.now())
                .viewCount(100L)
                .build();

        when(channelNodeRepository.findNodeOnlyByChannelId("ch-1")).thenReturn(Optional.empty());
        when(videoNodeRepository.findNodeOnlyByVideoId("yt-1")).thenReturn(Optional.empty());

        videoGraphSyncService.syncVideo(video);

        ArgumentCaptor<VideoNode> videoCaptor = ArgumentCaptor.forClass(VideoNode.class);
        verify(videoNodeRepository).save(videoCaptor.capture());

        VideoNode saved = videoCaptor.getValue();
        assertThat(saved.getCountryCode()).isEqualTo("KR");
        assertThat(saved.getLanguageCode()).isEqualTo("ko");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getTargetId()).isEqualTo(10L);

        verify(channelNodeRepository).save(any(ChannelNode.class));
    }

    @Test
    void syncVideo_skipsSave_whenCountryAndLanguageCannotBeResolved() {
        YoutubeVideo video = YoutubeVideo.builder()
                .id(11L)
                .youtubeVideoId("yt-2")
                .channelId("ch-2")
                .countryCode("XX")
                .defaultLanguageCode(null)
                .build();

        videoGraphSyncService.syncVideo(video);

        verify(videoNodeRepository, never()).save(any());
        verify(channelNodeRepository, never()).save(any());
    }
}
