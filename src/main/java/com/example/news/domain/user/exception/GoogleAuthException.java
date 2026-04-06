package com.example.news.domain.user.exception;

import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.global.exception.CustomException;

public class GoogleAuthException extends CustomException {
    public GoogleAuthException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
