package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {
    Optional<AnalysisJob> findByTargetIdAndJobType(Long targetId, JobType jobType);
    List<AnalysisJob> findAllByTargetIdAndTargetType(Long targetId, TargetType targetType);
}