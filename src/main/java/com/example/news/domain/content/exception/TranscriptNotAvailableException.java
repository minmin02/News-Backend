package com.example.news.domain.content.exception;

import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.global.exception.CustomException;

public class TranscriptNotAvailableException extends CustomException {
    public TranscriptNotAvailableException() {
        super(YoutubeErrorCode.TRANSCRIPT_NOT_AVAILABLE);
    }
}
