package com.example.news.domain.issue.service;

import com.example.news.domain.issue.dto.IssueCandidateDto;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.repository.ComparisonResultRepository;
import com.example.news.domain.issue.repository.IssueClusterItemRepository;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueComparisonService {

    private final IssueClusterRepository issueClusterRepository;
    private final IssueClusterItemRepository issueClusterItemRepository;
    private final ComparisonResultRepository comparisonResultRepository;

    // B-파트에서 구현 예정
    public List<IssueCluster> buildIssueClusters(List<IssueCandidateDto> candidates) {
        throw new UnsupportedOperationException("buildIssueClusters: not yet implemented");
    }

    // B-파트에서 구현 예정
    public List<IssueCandidateDto> prepareComparisonInputs(
            String keyword, List<String> countries, LocalDate start, LocalDate end) {
        throw new UnsupportedOperationException("prepareComparisonInputs: not yet implemented");
    }
}