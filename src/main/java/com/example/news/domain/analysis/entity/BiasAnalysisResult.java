package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "bias_analysis_result")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiasAnalysisResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BIAS_ANALYSIS_RESULT_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANALYSIS_JOB_ID")
    private AnalysisJob analysisJob;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    private Double overallBiasScore;

    private Double opinionScore;

    private Double emotionScore;

    private Double anonymousSourceScore;

    private Double headlineBodyGapScore;

    private Double neutralityScore;

    @Column(columnDefinition = "TEXT")
    private String summaryText;

    @Column(columnDefinition = "TEXT")
    private String perspectiveSummary;

    @Column(columnDefinition = "TEXT")
    private String evidenceSummary;

    private String toneLabel;

}