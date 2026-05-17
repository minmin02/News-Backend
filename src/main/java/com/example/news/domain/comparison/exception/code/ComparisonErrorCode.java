package com.example.news.domain.comparison.exception.code;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ComparisonErrorCode implements ResponseCode {

    INVALID_COMPARISON_REQUEST("CP001", "비교분석 요청값이 올바르지 않습니다."),
    COMPARISON_API_FAILED("CP002", "비교분석 Python API 호출에 실패했습니다."),
    COMPARISON_VIDEO_NOT_FOUND("CP003", "비교분석 영상 정보를 찾을 수 없습니다.");

    private final String statusCode;
    private final String message;
}
