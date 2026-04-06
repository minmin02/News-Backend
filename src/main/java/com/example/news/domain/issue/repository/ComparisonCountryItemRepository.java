package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.ComparisonCountryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparisonCountryItemRepository extends JpaRepository<ComparisonCountryItem, Long> {

    List<ComparisonCountryItem> findByComparisonResultId(Long comparisonResultId);

    void deleteByComparisonResultId(Long comparisonResultId);
}
