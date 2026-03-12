package com.example.news.domain.user.service;

import com.example.news.domain.user.converter.UserConverter;
import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.entity.User;
import com.example.news.domain.user.exception.EmailDuplicateException;
import com.example.news.domain.user.exception.InvalidCredentialsException;
import com.example.news.domain.user.repository.UserRepository;
import com.example.news.global.jwt.JwtUtil;
import com.example.news.global.jwt.dto.TokenDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.news.domain.user.exception.UserNotFoundException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        TokenDataDto tokenData = jwtUtil.createTokenData(user.getUserId());
        return UserDto.TokenResponseDto.from(tokenData);
    }

}