package com.example.news.domain.personalization.repository;

import com.example.news.domain.personalization.entity.Scrap;
import com.example.news.domain.personalization.enums.ScrapTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    List<Scrap> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, ScrapTargetType targetType);

    Optional<Scrap> findByIdAndUserId(Long id, Long userId);
}
