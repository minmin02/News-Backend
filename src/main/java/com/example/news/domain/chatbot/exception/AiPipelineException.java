package com.example.news.domain.chatbot.exception;

import com.example.news.domain.chatbot.exception.code.ChatbotErrorCode;
import com.example.news.global.exception.CustomException;

public class AiPipelineException extends CustomException {
    public AiPipelineException() {
        super(ChatbotErrorCode.AI_PIPELINE_ERROR);
    }
}
