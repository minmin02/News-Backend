package com.example.news.domain.comparison.exception;

import com.example.news.domain.comparison.exception.code.ComparisonErrorCode;
import com.example.news.global.exception.CustomException;
import lombok.Getter;

@Getter
public class ComparisonException extends CustomException {

    private final ComparisonErrorCode errorCode;

    public ComparisonException(ComparisonErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public ComparisonException(ComparisonErrorCode errorCode, String message) {
        super(errorCode, message);
        this.errorCode = errorCode;
    }

    public ComparisonException(ComparisonErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.errorCode = errorCode;
    }
}
