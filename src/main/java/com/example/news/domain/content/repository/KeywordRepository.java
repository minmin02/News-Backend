package com.example.news.domain.content.repository;

import com.example.news.domain.content.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    // 정규화된 키워드로 조회
    Optional<Keyword> findByNormalizedKeyword(String normalizedKeyword);
}
