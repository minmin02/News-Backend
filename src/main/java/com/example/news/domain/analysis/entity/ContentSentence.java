package com.example.news.domain.analysis.entity;

import com.example.news.domain.analysis.enums.SentenceTargetType;
import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.checker.units.qual.C;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "content_sentence")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentSentence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONTENT_SENTENCE_ID")
    private Long id;

    private Long targetId;

    @Enumerated(EnumType.STRING)
    private SentenceTargetType targetType;

    private Integer sentenceOrder;

    private Integer startOffset;

    private Integer endOffset;

    @Column(columnDefinition = "TEXT")
    private String sentenceText;

    private Long startTimeMs;

    private Long endTimeMs;
}