package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.SentenceLabelType;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "sentence_bias_label")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SentenceBiasLabel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SENTENCE_BIAS_LABEL_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONTENT_SENTENCE_ID")
    private ContentSentence contentSentence;


    // 분석 실행 이력
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ANALYSIS_JOB_ID")
    private AnalysisJob analysisJob;


    @Enumerated(EnumType.STRING)
    private SentenceLabelType labelType;

    private Double score;

}
