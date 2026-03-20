package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeCommentDto;
import com.example.news.domain.content.entity.YoutubeComment;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.domain.content.repository.YoutubeCommentRepository;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.global.exception.CustomException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
                .orElseThrow(() -> new CustomException(YoutubeErrorCode.VIDEO_NOT_FOUND));

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
                    .map(thread -> buildComment(thread, video))
                    .map(youtubeCommentRepository::save)
                    .collect(Collectors.toList());

            return saved.stream()
                    .map(YoutubeConverter::toCommentDto)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new CustomException(YoutubeErrorCode.YOUTUBE_API_ERROR, e.getMessage(), e);
        }
    }

    // 댓글 엔티티 생성
    private YoutubeComment buildComment(CommentThread thread, YoutubeVideo video) {
        Comment topComment = thread.getSnippet().getTopLevelComment();
        CommentSnippet snippet = topComment.getSnippet();

        return YoutubeComment.builder()
                .youtubeVideo(video)
                .externalCommentId(thread.getId())
                .authorName(snippet.getAuthorDisplayName())
                .content(snippet.getTextDisplay())
                .likeCount(snippet.getLikeCount())
                .publishedAt(parseDateTime(snippet.getPublishedAt()))
                .isTopComment(true)
                .build();
    }

    // 날짜 파싱
    private LocalDateTime parseDateTime(com.google.api.client.util.DateTime dateTime) {
        if (dateTime == null) return null;
        return ZonedDateTime.parse(dateTime.toStringRfc3339()).toLocalDateTime();
    }
}
