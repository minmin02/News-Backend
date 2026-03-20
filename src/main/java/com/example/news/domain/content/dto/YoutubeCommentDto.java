package com.example.news.domain.content.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class YoutubeCommentDto {
    // 댓글 응답 객체
    private String commentId;
    private String authorName;
    private String content;
    private Long likeCount;
    private LocalDateTime publishedAt;
    private Boolean isTopComment;
}
