package com.example.news.domain.chatbot.service;

import com.example.news.domain.chatbot.converter.ChatbotConverter;
import com.example.news.domain.chatbot.dto.ChatbotDto;
import com.example.news.domain.chatbot.entity.ChatMessage;
import com.example.news.domain.chatbot.entity.ChatSession;
import com.example.news.domain.chatbot.enums.MessageRole;
import com.example.news.domain.chatbot.exception.AiPipelineException;
import com.example.news.domain.chatbot.exception.ChatSessionNotFoundException;
import com.example.news.domain.chatbot.repository.ChatMessageRepository;
import com.example.news.domain.chatbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final WebClient webClient;

    @Value("${python.base-url}")
    private String pythonBaseUrl;

    private static final String DEFAULT_TITLE = "새 대화";
    private static final String BOT_NAME = "뉴스봇";
    private static final String BOT_PROFILE_IMAGE_URL = "";
    private static final String WELCOME_MESSAGE = "안녕하세요! 뉴스 편향 분석을 도와드리는 뉴스봇입니다. 궁금하신 내용을 아래에서 선택하거나 직접 질문해 주세요.";
    private static final List<String> GUIDE_ITEMS = List.of(
            "편향 점수가 뭔가요?",
            "이 영상 분석 결과 설명해줘",
            "한국 vs 미국 보도 차이 알려줘"
    );

    public ChatbotDto.WelcomeResponseDto getWelcome() {
        return new ChatbotDto.WelcomeResponseDto(
                BOT_NAME,
                BOT_PROFILE_IMAGE_URL,
                WELCOME_MESSAGE,
                GUIDE_ITEMS
        );
    }

    @Transactional
    public ChatbotDto.SessionResponseDto createSession(Long userId, ChatbotDto.CreateSessionRequestDto request) {
        String title = (request.title() != null && !request.title().isBlank())
                ? request.title()
                : DEFAULT_TITLE;

        ChatSession session = chatSessionRepository.save(ChatbotConverter.toChatSession(userId, title));
        return ChatbotConverter.toSessionResponseDto(session);
    }

    @Transactional(readOnly = true)
    public ChatbotDto.SessionListResponseDto getSessions(Long userId) {
        List<ChatSession> sessions = chatSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);

        List<ChatbotDto.SessionSummaryDto> summaries = sessions.stream()
                .map(session -> {
                    String lastMessage = chatMessageRepository
                            .findTopBySessionIdOrderByCreatedAtDesc(session.getId())
                            .map(ChatMessage::getContent)
                            .orElse(null);
                    return ChatbotConverter.toSessionSummaryDto(session, lastMessage);
                })
                .toList();

        return new ChatbotDto.SessionListResponseDto(summaries);
    }

    @Transactional(readOnly = true)
    public ChatbotDto.MessageListResponseDto getMessages(Long userId, Long sessionId) {
        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(ChatSessionNotFoundException::new);

        List<ChatbotDto.MessageResponseDto> messages = chatMessageRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(ChatbotConverter::toMessageResponseDto)
                .toList();

        return new ChatbotDto.MessageListResponseDto(sessionId, messages);
    }

    public ChatbotDto.SendMessageResponseDto sendMessage(Long userId, Long sessionId, ChatbotDto.SendMessageRequestDto request) {
        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(ChatSessionNotFoundException::new);

        ChatMessage userMessage = saveMessage(sessionId, MessageRole.USER, request.content());

        List<Map<String, String>> historyPayload = chatMessageRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> Map.of(
                        "role", m.getRole().name().toLowerCase(),
                        "content", m.getContent()
                ))
                .toList();

        String botContent = callAiPipeline(historyPayload);

        ChatMessage botMessage = saveMessage(sessionId, MessageRole.BOT, botContent);

        return new ChatbotDto.SendMessageResponseDto(
                ChatbotConverter.toMessageResponseDto(userMessage),
                ChatbotConverter.toMessageResponseDto(botMessage)
        );
    }

    @Transactional
    public ChatMessage saveMessage(Long sessionId, MessageRole role, String content) {
        return chatMessageRepository.save(ChatbotConverter.toChatMessage(sessionId, role, content));
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(ChatSessionNotFoundException::new);

        chatMessageRepository.deleteAllBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    private String callAiPipeline(List<Map<String, String>> history) {
        try {
            Map<String, Object> body = Map.of("messages", history);

            Map<?, ?> response = webClient.post()
                    .uri(pythonBaseUrl + "/chatbot/chat")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("reply")) {
                throw new AiPipelineException();
            }

            return (String) response.get("reply");
        } catch (AiPipelineException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI pipeline 호출 실패: {}", e.getMessage());
            throw new AiPipelineException();
        }
    }
}
