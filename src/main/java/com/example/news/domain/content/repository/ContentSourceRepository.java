package com.example.news.domain.content.repository;

import com.example.news.domain.content.entity.ContentSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentSourceRepository extends JpaRepository<ContentSource, Long> {
    // isFeatured=true 인 채널 목록 조회
    List<ContentSource> findByIsFeaturedTrue();
}
