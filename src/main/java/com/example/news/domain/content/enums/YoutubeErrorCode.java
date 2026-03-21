package com.example.news.domain.content.enums;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum YoutubeErrorCode implements ResponseCode {
    // ContentBC 전용 에러 코드, ResponseCode 구현
    // 이걸 CustomException에 이 에러를 던지면, GlobalExceptionHandler가 처리

    VIDEO_NOT_FOUND("YT001", "youtube video not found"), // 영상 없음
    TRANSCRIPT_NOT_AVAILABLE("YT002", "transcript not available for this video"), // 자막 없음
    YOUTUBE_API_ERROR("YT003", "youtube api request failed"), // API 호출 실패
    COMMENTS_DISABLED("YT004", "comments are disabled for this video"); // 댓글 비활성화

    private final String statusCode;
    private final String message;
}
