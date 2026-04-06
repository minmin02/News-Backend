package com.example.news.domain.user.exception;

import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.global.exception.CustomException;

public class KakaoAuthException extends CustomException {
    public KakaoAuthException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
