package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BiasAnalysisResultRepository extends JpaRepository<BiasAnalysisResult, Long> {
    Optional<BiasAnalysisResult> findByTargetIdAndTargetType(Long targetId, TargetType targetType);
}