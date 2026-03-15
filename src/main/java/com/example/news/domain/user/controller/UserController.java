package com.example.news.domain.user.controller;

import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.service.UserService;
import com.example.news.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ApiResponse<UserDto.TokenResponseDto> signup(@RequestBody  @Valid UserDto.SignupRequestDto request) {
        UserDto.TokenResponseDto response = userService.signup(request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/login")
    public ApiResponse<UserDto.TokenResponseDto> login(@RequestBody  @Valid UserDto.LoginRequestDto request) {
        UserDto.TokenResponseDto response = userService.login(request);
        return ApiResponse.ok(response);
    }
}
