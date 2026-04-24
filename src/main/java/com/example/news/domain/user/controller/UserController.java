package com.example.news.domain.user.controller;

import com.example.news.domain.user.dto.GoogleDto;
import com.example.news.domain.user.dto.KakaoDto;
import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.service.GoogleAuthService;
import com.example.news.domain.user.service.KakaoAuthService;
import com.example.news.domain.user.service.UserService;
import com.example.news.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final KakaoAuthService kakaoAuthService;
    private final GoogleAuthService googleAuthService;

    @PostMapping("/signup")
    public ApiResponse<UserDto.TokenResponseDto> signup(@RequestBody  @Valid UserDto.SignupRequestDto request) {
        UserDto.TokenResponseDto response = userService.signup(request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/login")
    public ApiResponse<UserDto.TokenResponseDto> login(@RequestBody @Valid UserDto.LoginRequestDto request) {
        UserDto.TokenResponseDto response = userService.login(request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/kakao")
    public ApiResponse<UserDto.TokenResponseDto> kakaoLogin(@RequestBody @Valid KakaoDto.LoginRequestDto request) {
        UserDto.TokenResponseDto response = kakaoAuthService.kakaoLogin(request.code());
        return ApiResponse.ok(response);
    }

    @PostMapping("/google")
    public ApiResponse<UserDto.TokenResponseDto> googleLogin(@RequestBody @Valid GoogleDto.LoginRequestDto request) {
        UserDto.TokenResponseDto response = googleAuthService.googleLogin(request.code());
        return ApiResponse.ok(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            userService.logout(bearer.substring(7));
        }
        return ApiResponse.ok();
    }
}
