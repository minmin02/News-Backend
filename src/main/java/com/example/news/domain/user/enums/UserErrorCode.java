package com.example.news.domain.user.enums;

import com.example.news.global.code.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ResponseCode {
    EMAIL_DUPLICATE("U001", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD("U002", "비밀번호가 일치하지 않습니다"),
    USER_NOT_FOUND("U003", "존재하지 않는 사용자입니다"),
    INVALID_CREDENTIALS("U004", "이메일 또는 비밀번호가 올바르지 않습니다"),
    PASSWORD_MISMATCH("U005", "비밀번호 확인이 일치하지 않습니다");
    private final String statusCode;
    private final String message;
}
