package com.example.news.domain.chatbot.exception;

import com.example.news.domain.chatbot.exception.code.ChatbotErrorCode;
import com.example.news.global.exception.CustomException;

public class ChatSessionNotFoundException extends CustomException {
    public ChatSessionNotFoundException() {
        super(ChatbotErrorCode.CHAT_SESSION_NOT_FOUND);
    }
}
