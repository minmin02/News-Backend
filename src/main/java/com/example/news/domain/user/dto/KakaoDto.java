package com.example.news.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class KakaoDto {

    public record LoginRequestDto(
            @NotBlank(message = "인가 코드는 필수입니다")
            String code
    ) {}

    // 카카오 토큰 교환 응답 파싱용 (내부 사용)
    public record KakaoTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    // 카카오 사용자 정보 응답 파싱용 (내부 사용)
    public record KakaoUserInfoResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        public record KakaoAccount(
                String email
        ) {}
    }
}
