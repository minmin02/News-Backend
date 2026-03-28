package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.HighlightSpan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HighlightSpanRepository extends JpaRepository<HighlightSpan, Long> {
    List<HighlightSpan> findByHighlightResultId(Long highlightResultId);
}
