package com.example.news.domain.issue.enums;

import com.example.news.domain.issue.exception.IssueErrorCode;
import com.example.news.domain.issue.exception.IssueException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum CountryCode {
    KR("한국", "한국어", "ko"),
    US("미국", "영어", "en"),
    CN("중국", "중국어", "zh-CN"),
    JP("일본", "일본어", "ja");

    private final String countryName;
    private final String languageLabel;
    private final String languageCode;

    public static CountryCode of(String code) {
        return Arrays.stream(values())
                .filter(c -> c.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IssueException(IssueErrorCode.UNSUPPORTED_COUNTRY, "countryCode: " + code));
    }
}
