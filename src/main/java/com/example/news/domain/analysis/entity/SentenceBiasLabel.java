package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.SentenceLabelType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sentence_bias_label")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SentenceBiasLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SENTENCE_BIAS_LABEL_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONTENT_SENTENCE_ID")
    private ContentSentence contentSentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BIAS_ANALYSIS_RESULT_ID")
    private BiasAnalysisResult biasAnalysisResult;

    @Enumerated(EnumType.STRING)
    private SentenceLabelType labelType;

    private Double score;

    private String highlightColor;

    private String evidenceKeyword;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
