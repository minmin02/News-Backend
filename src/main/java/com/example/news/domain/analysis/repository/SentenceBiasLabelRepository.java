package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.SentenceBiasLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceBiasLabelRepository extends JpaRepository<SentenceBiasLabel, Long> {
    // NOTE:
    // 현재 SentenceBiasLabel 엔티티는 biasAnalysisResult가 아니라 analysisJob과 연관되어 있음.
    // 따라서 조회 메서드도 실제 엔티티 필드에 맞춰 analysisJobId 기준으로 정의한다.
    //
    // 기존 findAllByBiasAnalysisResultId(...) 는 초기 문서 스펙 기준 이름이지만,
    // 현재 머지된 엔티티 구조와는 맞지 않아 메서드명을 정정한다.
    List<SentenceBiasLabel> findAllByAnalysisJobId(Long analysisJobId);
}
