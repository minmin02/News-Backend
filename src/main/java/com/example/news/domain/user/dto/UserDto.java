package com.example.news.domain.user.dto;

import com.example.news.global.jwt.dto.TokenDataDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDto {

    public record SignupRequestDto(
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다")
            String password
    ) {}

    public record LoginRequestDto(
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다")
            String password
    ) {}

    public record TokenResponseDto(
            String grantType,
            String accessToken,
            String refreshToken,
            Long accessTokenExpiredAt,
            Long refreshTokenExpiredAt
    ) {
        public static TokenResponseDto from(TokenDataDto tokenData) {
            return new TokenResponseDto(
                    tokenData.getGrantType(),
                    tokenData.getAccessToken(),
                    tokenData.getRefreshToken(),
                    tokenData.getAccessTokenExpiredAt(),
                    tokenData.getRefreshTokenExpiredAt()
            );
        }
    }
}