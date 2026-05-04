package com.example.news.domain.chatbot.converter;

import com.example.news.domain.chatbot.dto.ChatbotDto;
import com.example.news.domain.chatbot.entity.ChatMessage;
import com.example.news.domain.chatbot.entity.ChatSession;
import com.example.news.domain.chatbot.enums.MessageRole;

public class ChatbotConverter {

    public static ChatSession toChatSession(Long userId, String title) {
        return ChatSession.builder()
                .userId(userId)
                .title(title)
                .build();
    }

    public static ChatMessage toChatMessage(Long sessionId, MessageRole role, String content) {
        return ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .build();
    }

    public static ChatbotDto.SessionResponseDto toSessionResponseDto(ChatSession session) {
        return new ChatbotDto.SessionResponseDto(
                session.getId(),
                session.getTitle(),
                session.getCreatedAt()
        );
    }

    public static ChatbotDto.SessionSummaryDto toSessionSummaryDto(ChatSession session, String lastMessage) {
        return new ChatbotDto.SessionSummaryDto(
                session.getId(),
                session.getTitle(),
                lastMessage,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    public static ChatbotDto.MessageResponseDto toMessageResponseDto(ChatMessage message) {
        return new ChatbotDto.MessageResponseDto(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
