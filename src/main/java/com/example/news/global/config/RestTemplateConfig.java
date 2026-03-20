package com.example.news.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    // 일반 HTTP 요청을 보낼 수 있는 클라이언트 객체를 Spring에 등록
    // 이게 없으면 자막 서비스에서 RestTemplate restTemplate 주입이 안 됨.
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
