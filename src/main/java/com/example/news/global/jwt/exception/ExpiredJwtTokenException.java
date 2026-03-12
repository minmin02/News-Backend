package com.example.news.global.jwt.exception;


import com.example.news.global.exception.CustomException;
import com.example.news.global.jwt.enums.JwtErrorCode;

public class ExpiredJwtTokenException extends CustomException {
    public ExpiredJwtTokenException() {
        super(JwtErrorCode.EXPIRED_JWT_TOKEN);
    }
}