package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.EvidenceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bias_evidence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BiasEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BIAS_EVIDENCE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BIAS_ANALYSIS_RESULT_ID")
    private BiasAnalysisResult biasAnalysisResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONTENT_SENTENCE_ID")
    private ContentSentence contentSentence;

    @Enumerated(EnumType.STRING)
    private EvidenceType evidenceType;

    private String title;

    private String description;

    private String sourceText;

    private Double confidenceScore;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
