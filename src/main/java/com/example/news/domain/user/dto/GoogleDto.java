package com.example.news.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class GoogleDto {

    public record LoginRequestDto(
            @NotBlank(message = "인가 코드는 필수입니다")
            String code
    ) {}

    // 구글 토큰 교환 응답 파싱용 (내부 사용)
    public record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    // 구글 사용자 정보 응답 파싱용 (내부 사용)
    public record GoogleUserInfoResponse(
            String id,
            String email
    ) {}
}
