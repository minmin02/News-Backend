package com.example.news.domain.personalization.exception.code;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PersonalizationErrorCode implements ResponseCode {
    SCRAP_NOT_FOUND("P001", "스크랩을 찾을 수 없습니다"),
    DUPLICATE_SCRAP("P002", "이미 스크랩한 콘텐츠입니다");

    private final String statusCode;
    private final String message;
}
