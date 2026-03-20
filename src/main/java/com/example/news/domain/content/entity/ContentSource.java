package com.example.news.domain.content.entity;

import com.example.news.domain.content.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "content_source")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentSource {
    // 방송사, 채널  메타 정보 저장 테이블
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String displayName;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String countryCode;

    private String youtubeChannelId;

    private String youtubeChannelUrl;

    @Builder.Default
    private Boolean isFeatured = false;

    private String logoUrl;

    private String homepageUrl;
}
