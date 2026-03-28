package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.SentenceBiasLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceBiasLabelRepository extends JpaRepository<SentenceBiasLabel, Long> {
    List<SentenceBiasLabel> findAllByAnalysisJobId(Long analysisJobId);
}
