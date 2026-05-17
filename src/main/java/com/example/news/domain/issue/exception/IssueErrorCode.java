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
    COMPARISON_RESULT_NOT_FOUND("IS005", "comparison result not found"),
    ANALYSIS_NOT_COMPLETED("IS006", "analysis not completed for this video"),
    OPPOSING_VIDEO_NOT_FOUND("IS007", "no opposing video found in the same cluster"),
    INVALID_CLUSTER_STATUS("IS008", "invalid issue cluster status transition"),
    CURATION_ITEM_DUPLICATE("IS009", "curation item already exists in set"),
    INVALID_COUNTRY_SCOPE("IS010", "curation country must be one of KR, US, CN");

    private final String statusCode;
    private final String message;
}
