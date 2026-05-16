package com.example.news.domain.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCandidateDto {

    private Long videoId;
    private String countryCode;
    private String title;
    private String summaryText;
    private Double overallBiasScore;
    private Double opinionScore;
    private List<String> analysisKeywords;
    private LocalDateTime publishedAt;
}