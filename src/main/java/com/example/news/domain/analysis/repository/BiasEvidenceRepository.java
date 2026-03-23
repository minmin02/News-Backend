package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.BiasEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BiasEvidenceRepository extends JpaRepository<BiasEvidence, Long> {
    List<BiasEvidence> findAllByBiasAnalysisResultId(Long biasAnalysisResultId);
}
