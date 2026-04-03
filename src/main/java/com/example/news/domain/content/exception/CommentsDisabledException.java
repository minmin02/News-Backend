package com.example.news.domain.content.exception;

import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.global.exception.CustomException;

public class CommentsDisabledException extends CustomException {
    public CommentsDisabledException() {
        super(YoutubeErrorCode.COMMENTS_DISABLED);
    }
}
