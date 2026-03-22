package com.example.news.domain.analysis.exception;

import com.example.news.domain.analysis.exception.code.AnalysisErrorCode;
import com.example.news.global.exception.CustomException;
import lombok.Getter;

@Getter
public class AnalysisException extends CustomException {

    private final AnalysisErrorCode errorCode;

    public AnalysisException(AnalysisErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public AnalysisException(AnalysisErrorCode errorCode, String message) {
        super(errorCode, message);
        this.errorCode = errorCode;
    }

    public AnalysisException(AnalysisErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.errorCode = errorCode;
    }
}
