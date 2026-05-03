package com.example.news.domain.chatbot.dto;

import com.example.news.domain.chatbot.enums.MessageRole;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

public class ChatbotDto {

    public record CreateSessionRequestDto(
            String title
    ) {}

    public record SessionResponseDto(
            Long sessionId,
            String title,
            LocalDateTime createdAt
    ) {}

    public record SessionSummaryDto(
            Long sessionId,
            String title,
            String lastMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record SessionListResponseDto(
            List<SessionSummaryDto> sessions
    ) {}

    public record MessageResponseDto(
            Long messageId,
            MessageRole role,
            String content,
            LocalDateTime createdAt
    ) {}

    public record MessageListResponseDto(
            Long sessionId,
            List<MessageResponseDto> messages
    ) {}

    public record SendMessageRequestDto(
            @NotBlank(message = "content는 필수입니다")
            String content
    ) {}

    public record SendMessageResponseDto(
            MessageResponseDto userMessage,
            MessageResponseDto botMessage
    ) {}

    public record WelcomeResponseDto(
            String botName,
            String botProfileImageUrl,
            String welcomeMessage,
            List<String> guideItems
    ) {}
}
