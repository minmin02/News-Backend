package com.example.news.domain.user.converter;

import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserConverter {

    public static User toUser(UserDto.SignupRequestDto request, PasswordEncoder passwordEncoder) {
        return User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();
    }

    public static UserDto.ProfileResponseDto toProfileResponseDto(User user) {
        return UserDto.ProfileResponseDto.from(user);
    }
}
