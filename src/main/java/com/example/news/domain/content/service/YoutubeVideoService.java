package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.global.exception.CustomException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.VideoListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YoutubeVideoService {
    // 영상 단건 상세 조회 서비스

    private final YouTube youtubeClient;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final YoutubeSearchService youtubeSearchService;

    @Value("${youtube.api.key}")
    private String apiKey;

    // DB에 있ㅇ으면 바로 반환
    @Transactional
    public YoutubeVideoDto.VideoDetail getVideo(String youtubeVideoId) {
        YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
                .orElseGet(() -> fetchFromApi(youtubeVideoId));
        return YoutubeConverter.toVideoDetail(video);
    }

    // API에서 영상 직접 조회
    private YoutubeVideo fetchFromApi(String youtubeVideoId) {
        try {
            YouTube.Videos.List request = youtubeClient.videos()
                    .list(List.of("snippet", "contentDetails", "statistics"));
            request.setKey(apiKey);
            request.setId(List.of(youtubeVideoId));

            VideoListResponse response = request.execute();
            if (response.getItems() == null || response.getItems().isEmpty()) {
                throw new CustomException(YoutubeErrorCode.VIDEO_NOT_FOUND);
            }

            // YoutubeSearchService.saveVideo() 재사용해서 저장 후 반환
            return youtubeSearchService.saveVideo(response.getItems().get(0));
        } catch (IOException e) {
            throw new CustomException(YoutubeErrorCode.YOUTUBE_API_ERROR, e.getMessage(), e);
        }
    }
}
