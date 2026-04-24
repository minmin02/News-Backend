package com.example.news.domain.user.service;

import com.example.news.domain.user.converter.UserConverter;
import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.entity.User;
import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.domain.user.enums.UserStatus;
import com.example.news.domain.user.exception.EmailDuplicateException;
import com.example.news.domain.user.exception.InvalidCredentialsException;
import com.example.news.domain.user.exception.UserNotFoundException;
import com.example.news.domain.user.repository.UserRepository;
import com.example.news.global.exception.CustomException;
import com.example.news.global.jwt.JwtUtil;
import com.example.news.global.jwt.dto.TokenDataDto;
import com.example.news.global.jwt.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public UserDto.TokenResponseDto signup(UserDto.SignupRequestDto request) {

        if (userRepository.existsByEmail(request.email())) {throw new EmailDuplicateException();}

        User user = UserConverter.toUser(request, passwordEncoder);
        userRepository.save(user);
        TokenDataDto tokenData = jwtUtil.createTokenData(user.getUserId());

        return UserDto.TokenResponseDto.from(tokenData);
    }

    @Transactional(readOnly = true)
    public UserDto.TokenResponseDto login(UserDto.LoginRequestDto request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(UserNotFoundException::new);

        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(UserErrorCode.ALREADY_WITHDRAWN);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        TokenDataDto tokenData = jwtUtil.createTokenData(user.getUserId());
        return UserDto.TokenResponseDto.from(tokenData);
    }

    @Transactional(readOnly = true)
    public UserDto.ProfileResponseDto getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return UserConverter.toProfileResponseDto(user);
    }

    @Transactional
    public UserDto.ProfileResponseDto updateMyProfile(Long userId, UserDto.ProfileUpdateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(UserErrorCode.ALREADY_WITHDRAWN);
        }
        user.updateProfile(request.name(), request.nickname(), request.birth(), request.phone());
        return UserConverter.toProfileResponseDto(user);
    }

    @Transactional
    public void withdraw(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new CustomException(UserErrorCode.ALREADY_WITHDRAWN);
        }
        user.withdraw();
        tokenBlacklistService.invalidateUserTokens(userId);
        jwtUtil.blacklistToken(accessToken);
    }

    public void logout(String accessToken) {
        jwtUtil.blacklistToken(accessToken);
    }
}