package com.example.news.global.jwt.exception;


import com.example.news.global.exception.CustomException;
import com.example.news.global.jwt.enums.JwtErrorCode;

public class JwtClaimsEmptyException extends CustomException {
    public JwtClaimsEmptyException() {
        super(JwtErrorCode.JWT_CLAIMS_EMPTY);
    }
}