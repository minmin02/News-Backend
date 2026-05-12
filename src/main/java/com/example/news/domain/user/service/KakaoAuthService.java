package com.example.news.domain.user.service;

import com.example.news.domain.user.dto.KakaoDto;
import com.example.news.domain.user.dto.UserDto;
import com.example.news.domain.user.entity.User;
import com.example.news.domain.user.enums.SocialProvider;
import com.example.news.domain.user.enums.UserErrorCode;
import com.example.news.domain.user.exception.KakaoAuthException;
import com.example.news.domain.user.repository.UserRepository;
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
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Transactional
    public UserDto.TokenResponseDto kakaoLogin(String code) {
        String kakaoAccessToken = getKakaoAccessToken(code);
        KakaoDto.KakaoUserInfoResponse userInfo = getKakaoUserInfo(kakaoAccessToken);

        String providerUserId = String.valueOf(userInfo.id());
        String providerEmail = userInfo.kakaoAccount() != null ? userInfo.kakaoAccount().email() : null;

        User user = userRepository
                .findByProviderAndProviderUserId(SocialProvider.KAKAO, providerUserId)
                .orElseGet(() -> createSocialUser(SocialProvider.KAKAO, providerUserId, providerEmail));

        TokenDataDto tokenData = jwtUtil.createTokenData(user.getUserId());
        return UserDto.TokenResponseDto.from(tokenData);
    }

    private String getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        try {
            log.info(
                    "카카오 토큰 교환 요청: clientId={}, redirectUri={}, clientSecretConfigured={}, codeLength={}",
                    maskClientId(clientId),
                    redirectUri,
                    hasText(clientSecret),
                    code != null ? code.length() : 0
            );

            ResponseEntity<KakaoDto.KakaoTokenResponse> response = restTemplate.postForEntity(
                    KAKAO_TOKEN_URL,
                    new HttpEntity<>(params, headers),
                    KakaoDto.KakaoTokenResponse.class
            );

            KakaoDto.KakaoTokenResponse body = response.getBody();
            if (body == null || !hasText(body.accessToken())) {
                log.error("카카오 토큰 교환 응답이 비어있습니다. status={}", response.getStatusCode());
                throw new KakaoAuthException(UserErrorCode.KAKAO_AUTH_FAILED);
            }

            log.info("카카오 토큰 교환 성공: status={}", response.getStatusCode());
            return body.accessToken();
        } catch (RestClientResponseException e) {
            log.error(
                    "카카오 토큰 교환 실패: status={}, responseBody={}, clientId={}, redirectUri={}, clientSecretConfigured={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    maskClientId(clientId),
                    redirectUri,
                    hasText(clientSecret)
            );
            throw new KakaoAuthException(UserErrorCode.KAKAO_AUTH_FAILED);
        } catch (Exception e) {
            log.error(
                    "카카오 토큰 교환 실패: message={}, clientId={}, redirectUri={}, clientSecretConfigured={}",
                    e.getMessage(),
                    maskClientId(clientId),
                    redirectUri,
                    hasText(clientSecret),
                    e
            );
            throw new KakaoAuthException(UserErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    private KakaoDto.KakaoUserInfoResponse getKakaoUserInfo(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        try {
            ResponseEntity<KakaoDto.KakaoUserInfoResponse> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoDto.KakaoUserInfoResponse.class
            );

            KakaoDto.KakaoUserInfoResponse body = response.getBody();
            if (body == null || body.id() == null) {
                log.error("카카오 사용자 정보 응답이 비어있습니다. status={}", response.getStatusCode());
                throw new KakaoAuthException(UserErrorCode.KAKAO_USER_INFO_FAILED);
            }

            log.info("카카오 사용자 정보 조회 성공: kakaoUserId={}", body.id());
            return body;
        } catch (RestClientResponseException e) {
            log.error("카카오 사용자 정보 조회 실패: status={}, responseBody={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new KakaoAuthException(UserErrorCode.KAKAO_USER_INFO_FAILED);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패: {}", e.getMessage(), e);
            throw new KakaoAuthException(UserErrorCode.KAKAO_USER_INFO_FAILED);
        }
    }

    private User createSocialUser(SocialProvider provider, String providerUserId, String providerEmail) {
        return userRepository.save(User.builder()
                .email("kakao_" + providerUserId + "@social.local")
                .provider(provider)
                .providerUserId(providerUserId)
                .build());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String maskClientId(String value) {
        if (!hasText(value) || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }
}
