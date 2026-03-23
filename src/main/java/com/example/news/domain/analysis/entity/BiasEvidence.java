package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.EvidenceType;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "bias_evidence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BiasEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BIAS_EVIDENCE_ID")
    private Long id;

    //편향분석결과 엔티티
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BIAS_ANALYSIS_RESULT_ID")
    private BiasAnalysisResult biasAnalysisResult;

    // 컨텐츠 문장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONTENT_SENTENCE_ID")
    private ContentSentence contentSentence;

    //근거 유형
    @Enumerated(EnumType.STRING)
    private EvidenceType evidenceType;

    private String title;

    private String description;

    private String sourceText;

    private Double confidenceScore;

}
