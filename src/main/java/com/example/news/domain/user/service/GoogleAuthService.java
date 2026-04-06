package com.example.news.domain.user.service;

import com.example.news.domain.user.dto.GoogleDto;
import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.entity.User;
import com.example.news.domain.user.entity.UserSocialAccount;
import com.example.news.domain.user.enums.SocialProvider;
import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.domain.user.exception.GoogleAuthException;
import com.example.news.domain.user.repository.UserRepository;
import com.example.news.domain.user.repository.UserSocialAccountRepository;
import com.example.news.global.jwt.JwtUtil;
import com.example.news.global.jwt.dto.TokenDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    @Transactional
    public UserDto.TokenResponseDto googleLogin(String code) {
        String googleAccessToken = getGoogleAccessToken(code);
        GoogleDto.GoogleUserInfoResponse userInfo = getGoogleUserInfo(googleAccessToken);

        User user = userSocialAccountRepository
                .findByProviderAndProviderUserId(SocialProvider.GOOGLE, userInfo.id())
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> createSocialUser(userInfo.id(), userInfo.email()));

        TokenDataDto tokenData = jwtUtil.createTokenData(user.getUserId());
        return UserDto.TokenResponseDto.from(tokenData);
    }

    private String getGoogleAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            ResponseEntity<GoogleDto.GoogleTokenResponse> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL,
                    new HttpEntity<>(params, headers),
                    GoogleDto.GoogleTokenResponse.class
            );
            return response.getBody().accessToken();
        } catch (Exception e) {
            log.error("구글 토큰 교환 실패: {}", e.getMessage());
            throw new GoogleAuthException(UserErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    private GoogleDto.GoogleUserInfoResponse getGoogleUserInfo(String googleAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleAccessToken);

        try {
            ResponseEntity<GoogleDto.GoogleUserInfoResponse> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    GoogleDto.GoogleUserInfoResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("구글 사용자 정보 조회 실패: {}", e.getMessage());
            throw new GoogleAuthException(UserErrorCode.GOOGLE_USER_INFO_FAILED);
        }
    }

    private User createSocialUser(String providerUserId, String providerEmail) {
        User user;
        if (providerEmail != null && userRepository.existsByEmail(providerEmail)) {
            user = userRepository.findByEmail(providerEmail)
                    .orElseThrow(() -> new GoogleAuthException(UserErrorCode.GOOGLE_AUTH_FAILED));
        } else {
            String email = providerEmail != null
                    ? providerEmail
                    : "google_" + providerUserId + "@social.local";
            user = userRepository.save(User.builder()
                    .email(email)
                    .build());
        }

        userSocialAccountRepository.save(UserSocialAccount.builder()
                .user(user)
                .provider(SocialProvider.GOOGLE)
                .providerUserId(providerUserId)
                .providerEmail(providerEmail)
                .connectedAt(LocalDateTime.now())
                .build());

        return user;
    }
}
