package com.example.news.domain.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "keyword")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Keyword {
    // 검색 키워드 테이블
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String keywordName;

    @Column(unique = true, nullable = false)
    private String normalizedKeyword;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
