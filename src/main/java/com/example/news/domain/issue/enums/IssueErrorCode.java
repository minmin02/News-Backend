package com.example.news.domain.issue.enums;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IssueErrorCode implements ResponseCode {

    TRANSLATION_FAILED("IS001", "keyword translation failed"),
    UNSUPPORTED_COUNTRY("IS002", "unsupported country code"),
    ISSUE_NOT_FOUND("IS003", "issue cluster not found");

    private final String statusCode;
    private final String message;
}
