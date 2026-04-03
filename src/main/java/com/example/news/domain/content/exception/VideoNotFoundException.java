package com.example.news.domain.content.exception;

import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.global.exception.CustomException;

public class VideoNotFoundException extends CustomException {
    public VideoNotFoundException() {
        super(YoutubeErrorCode.VIDEO_NOT_FOUND);
    }
}
