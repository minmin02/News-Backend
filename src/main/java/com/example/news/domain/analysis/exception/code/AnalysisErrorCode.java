package com.example.news.domain.analysis.exception.code;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AnalysisErrorCode implements ResponseCode {

    ANALYSIS_JOB_FAILED("A001", "분석 작업 실행 중 오류가 발생했습니다."),
    ANALYSIS_JOB_NOT_FOUND("A002", "분석 작업을 찾을 수 없습니다.");

    private final String statusCode;
    private final String message;
}