package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.SentenceLabelType;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "highlight_span")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HighlightSpan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HIGHLIGHT_SPAN_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HIGHLIGHT_RESULT_ID")
    private HighlightResult highlightResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONTENT_SENTENCE_ID")
    private ContentSentence contentSentence;

    private Integer startOffset;

    private Integer endOffset;

    @Enumerated(EnumType.STRING)
    private SentenceLabelType labelType;

    private Double score;

    private String matchedWord;
}
