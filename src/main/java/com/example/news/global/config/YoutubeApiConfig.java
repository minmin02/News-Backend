package com.example.news.global.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YoutubeApiConfig {
    // Youtube Api 를 사용할 수 있는 클라이언트 객체를 만들어서 Spring에 등록
    // 이게 없으면 YoutubeSearchService, YoutubeVideoService 등에서
    // YouTube youtubeClient를 주입받지 못해서 앱 실행 자체가 안 됨.
    @Bean
    public YouTube youtubeClient() throws Exception {
        return new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), // HTTPS 통신 설정
                GsonFactory.getDefaultInstance(), // API 응답 JSON을 Java 객체로 변환하는 파서
                request -> {} // 요청 초기화 콜백
        ).setApplicationName("news-backend").build(); // 구글 쿼터 추적용 앱 이름
    }
}
