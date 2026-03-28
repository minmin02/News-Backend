package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.HighlightResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HighlightResultRepository extends JpaRepository<HighlightResult, Long> {
    Optional<HighlightResult> findByBiasAnalysisResultId(Long biasAnalysisResultId);
}
