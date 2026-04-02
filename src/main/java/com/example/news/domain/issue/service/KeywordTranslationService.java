package com.example.news.domain.issue.service;

import com.example.news.domain.issue.enums.IssueErrorCode;
import com.example.news.global.exception.CustomException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeywordTranslationService {

    private static final String TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";

    // 국가코드에서 언어코드로 매핑
    private static final Map<String, String> COUNTRY_TO_LANG = Map.of(
            "KR", "ko",
            "US", "en",
            "CN", "zh-CN",
            "JP", "ja"
    );

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.translate.api-key}")
    private String apiKey;

    // 한국어 키워드를 각 국가 언어로 번역해서 Map<국가코드, 번역된키워드> 반환
    // KR은 번역 없이 원본 그대로 반환
    public Map<String, String> translate(String keyword, List<String> countryCodes) {
        Map<String, String> result = new HashMap<>();

        for (String countryCode : countryCodes) {
            String lang = COUNTRY_TO_LANG.get(countryCode);
            if (lang == null) {
                throw new CustomException(IssueErrorCode.UNSUPPORTED_COUNTRY, "countryCode: " + countryCode);
            }

            // KR(한국어)는 번역 불필요
            if ("ko".equals(lang)) {
                result.put(countryCode, keyword);
                continue;
            }

            result.put(countryCode, callTranslateApi(keyword, lang));
        }

        return result;
    }

    // 구글 translate api에서 실제 HTTP 호출
    private String callTranslateApi(String keyword, String targetLang) {
        String url = UriComponentsBuilder.fromHttpUrl(TRANSLATE_URL)
                .queryParam("q", keyword)
                .queryParam("source", "ko")
                .queryParam("target", targetLang)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            return root.path("data")
                    .path("translations")
                    .get(0)
                    .path("translatedText")
                    .asText();
        } catch (Exception e) {
            throw new CustomException(IssueErrorCode.TRANSLATION_FAILED, e.getMessage(), e);
        }
    }
}
