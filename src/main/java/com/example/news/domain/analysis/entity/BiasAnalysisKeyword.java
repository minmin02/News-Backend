package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.BiasKeywordType;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "bias_analysis_keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BiasAnalysisKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BIAS_ANALYSIS_KEYWORD_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BIAS_ANALYSIS_RESULT_ID")
    private BiasAnalysisResult biasAnalysisResult;

    private String keywordText;

    @Enumerated(EnumType.STRING)
    private BiasKeywordType keywordType;

    private Double score;

}
