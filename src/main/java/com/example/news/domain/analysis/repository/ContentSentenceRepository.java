package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.ContentSentence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentSentenceRepository extends JpaRepository<ContentSentence, Long> {
    List<ContentSentence> findAllByTargetIdOrderBySentenceOrder(Long targetId);
    void deleteAllByTargetId(Long targetId);
}