package com.example.news.domain.content.repository;

import com.example.news.domain.content.entity.ContentSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentSourceRepository extends JpaRepository<ContentSource, Long> {
    // isFeatured=true 인 채널 목록 조회
    List<ContentSource> findByIsFeaturedTrue();

    // YouTube 채널 ID로 단건 조회
    Optional<ContentSource> findByYoutubeChannelId(String youtubeChannelId);
}
