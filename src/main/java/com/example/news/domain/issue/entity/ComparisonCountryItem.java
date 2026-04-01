package com.example.news.domain.issue.entity;

import com.example.news.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 국가별 비교 항목 — ComparisonResult를 국가 단위로 분리한 레코드.
 * 각 국가의 대표 영상 ID, 제목, 요약문, 편향 점수(biasScore)를 저장하며
 * Personalization BC에 메타데이터를 제공할 때 이 테이블을 기준으로 응답을 구성한다.
 */
@Entity
@Table(name = "comparison_country_item")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonCountryItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPARISON_COUNTRY_ITEM_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comparison_result_id")
    private ComparisonResult comparisonResult;

    private String countryCode;

    private Long representativeVideoId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String summaryText;

    private Double biasScore;
}