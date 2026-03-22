package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.BiasAnalysisKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BiasAnalysisKeywordRepository extends JpaRepository<BiasAnalysisKeyword, Long> {
    List<BiasAnalysisKeyword> findAllByBiasAnalysisResultId(Long biasAnalysisResultId);
}
