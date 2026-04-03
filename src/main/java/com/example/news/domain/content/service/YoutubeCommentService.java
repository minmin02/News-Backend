package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeCommentDto;
import com.example.news.domain.content.entity.YoutubeComment;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.exception.VideoNotFoundException;
import com.example.news.domain.content.exception.YoutubeApiException;
import com.example.news.domain.content.repository.YoutubeCommentRepository;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YoutubeCommentService {
    // 댓글 조회 서비스

    private final YouTube youtubeClient;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final YoutubeCommentRepository youtubeCommentRepository;

    @Value("${youtube.api.key}")
    private String apiKey;

    // 댓글 목록 조회
    @Transactional
    public List<YoutubeCommentDto> getComments(String youtubeVideoId) {
        YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
                .orElseThrow(VideoNotFoundException::new);

        List<YoutubeComment> existingComments =
                youtubeCommentRepository.findByYoutubeVideoOrderByLikeCountDesc(video);
        if (!existingComments.isEmpty()) {
            return existingComments.stream()
                    .map(YoutubeConverter::toCommentDto)
                    .collect(Collectors.toList());
        }

        return fetchAndSaveComments(video);
    }

    // 댓글 api 호출 및 저장
    private List<YoutubeCommentDto> fetchAndSaveComments(YoutubeVideo video) {
        try {
            YouTube.CommentThreads.List request = youtubeClient.commentThreads()
                    .list(List.of("snippet"));
            request.setKey(apiKey);
            request.setVideoId(video.getYoutubeVideoId());
            request.setMaxResults(50L);
            request.setOrder("relevance");

            CommentThreadListResponse response = request.execute();
            if (response.getItems() == null || response.getItems().isEmpty()) {
                return List.of();
            }

            List<YoutubeComment> saved = response.getItems().stream()
                    .filter(thread -> !youtubeCommentRepository
                            .existsByExternalCommentId(thread.getId()))
                    .map(thread -> YoutubeConverter.toCommentEntity(thread, video))
                    .map(youtubeCommentRepository::save)
                    .collect(Collectors.toList());

            return saved.stream()
                    .map(YoutubeConverter::toCommentDto)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new YoutubeApiException(e.getMessage(), e);
        }
    }

}
