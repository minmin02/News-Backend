package com.example.news.domain.issue.exception;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IssueErrorCode implements ResponseCode {

    TRANSLATION_FAILED("IS001", "keyword translation failed"),
    UNSUPPORTED_COUNTRY("IS002", "unsupported country code"),
    VIDEO_NOT_FOUND("IS003", "youtube video not found"),
    ISSUE_CLUSTER_NOT_FOUND("IS004", "issue cluster not found"),
    COMPARISON_RESULT_NOT_FOUND("IS005", "comparison result not found");

    private final String statusCode;
    private final String message;
}
