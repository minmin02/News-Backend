package com.example.news.domain.user.exception;


import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.global.exception.CustomException;

public class EmailDuplicateException extends CustomException {
    public EmailDuplicateException() {
        super(UserErrorCode.EMAIL_DUPLICATE);
    }
}