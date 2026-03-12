package com.example.news.global.jwt.exception;


import com.example.news.global.exception.CustomException;
import com.example.news.global.jwt.enums.JwtErrorCode;

public class InvalidJwtTokenException extends CustomException {
    public InvalidJwtTokenException() {
        super(JwtErrorCode.INVALID_JWT_TOKEN);
    }
}