package com.example.news.domain.user.controller;

import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.service.UserService;
import com.example.news.global.response.ApiResponse;
import com.example.news.global.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserDto.ProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ApiResponse.ok(userService.getMyProfile(userDetails.getUserId()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserDto.ProfileResponseDto> updateMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UserDto.ProfileUpdateRequestDto request) {
        return ApiResponse.ok(userService.updateMyProfile(userDetails.getUserId(), request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            HttpServletRequest request) {
        String token = extractToken(request);
        userService.withdraw(userDetails.getUserId(), token);
        return ApiResponse.ok();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        userService.logout(token);
        return ApiResponse.ok();
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
