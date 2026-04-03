package com.example.news.domain.content.exception;

import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.global.exception.CustomException;

public class YoutubeApiException extends CustomException {
    public YoutubeApiException() {
        super(YoutubeErrorCode.YOUTUBE_API_ERROR);
    }

    public YoutubeApiException(String message, Throwable cause) {
        super(YoutubeErrorCode.YOUTUBE_API_ERROR, message, cause);
    }
}
