package com.example.news.domain.chatbot.controller;

import com.example.news.domain.chatbot.dto.ChatbotDto;
import com.example.news.domain.chatbot.service.ChatbotService;
import com.example.news.global.response.ApiResponse;
import com.example.news.global.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/welcome")
    public ApiResponse<ChatbotDto.WelcomeResponseDto> getWelcome() {
        return ApiResponse.ok(chatbotService.getWelcome());
    }

    @PostMapping("/sessions")
    public ApiResponse<ChatbotDto.SessionResponseDto> createSession(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) ChatbotDto.CreateSessionRequestDto request) {
        ChatbotDto.CreateSessionRequestDto req = request != null ? request : new ChatbotDto.CreateSessionRequestDto(null);
        return ApiResponse.ok(chatbotService.createSession(userDetails.getUserId(), req));
    }

    @GetMapping("/sessions")
    public ApiResponse<ChatbotDto.SessionListResponseDto> getSessions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResponse.ok(chatbotService.getSessions(userDetails.getUserId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<ChatbotDto.MessageListResponseDto> getMessages(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long sessionId) {
        return ApiResponse.ok(chatbotService.getMessages(userDetails.getUserId(), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<ChatbotDto.SendMessageResponseDto> sendMessage(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long sessionId,
            @RequestBody @Valid ChatbotDto.SendMessageRequestDto request) {
        return ApiResponse.ok(chatbotService.sendMessage(userDetails.getUserId(), sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long sessionId) {
        chatbotService.deleteSession(userDetails.getUserId(), sessionId);
        return ApiResponse.ok();
    }
}
