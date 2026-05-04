package com.example.news.domain.chatbot.exception.code;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatbotErrorCode implements ResponseCode {
    CHAT_SESSION_NOT_FOUND("CB001", "채팅 세션을 찾을 수 없습니다"),
    CHAT_SESSION_UNAUTHORIZED("CB002", "해당 채팅 세션에 접근 권한이 없습니다"),
    AI_PIPELINE_ERROR("CB003", "AI 응답 생성에 실패했습니다");

    private final String statusCode;
    private final String message;
}
