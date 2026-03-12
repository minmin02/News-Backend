package com.example.news.global.jwt.exception;


import com.example.news.global.exception.CustomException;
import com.example.news.global.jwt.enums.JwtErrorCode;

public class UnsupportedJwtTokenException extends CustomException {
    public UnsupportedJwtTokenException() {
        super(JwtErrorCode.UNSUPPORTED_JWT_TOKEN);
    }
}