package com.example.news.domain.issue.exception;

import com.example.news.global.exception.CustomException;
import lombok.Getter;

@Getter
public class IssueException extends CustomException {
    private final IssueErrorCode errorCode;

    public IssueException(IssueErrorCode errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public IssueException(IssueErrorCode errorCode, String message) {
        super(errorCode, message);
        this.errorCode = errorCode;
    }

    public IssueException(IssueErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.errorCode = errorCode;
    }
}
