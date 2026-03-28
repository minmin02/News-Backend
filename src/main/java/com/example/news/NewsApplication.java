package com.example.news;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// created_at, updated_at 자동 입력 가능
@EnableJpaAuditing
@SpringBootApplication
public class

NewsApplication {
	//수정
	public static void main(String[] args) {
		SpringApplication.run(NewsApplication.class, args);
	}
}
