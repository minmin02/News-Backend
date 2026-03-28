package com.example.news.domain.analysis.repository;

import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.entity.ContentSentence;
import com.example.news.domain.analysis.entity.HighlightResult;
import com.example.news.domain.analysis.entity.HighlightSpan;
import com.example.news.domain.analysis.entity.SentenceBiasLabel;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.SentenceLabelType;
import com.example.news.domain.analysis.enums.SentenceTargetType;
import com.example.news.domain.analysis.enums.TargetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest는 NewsApplication을 bootstrap으로 로드하므로
// @EnableJpaAuditing이 이미 활성화됨 → BaseEntity의 createdAt/updatedAt 자동 처리
@DataJpaTest
class AnalysisRepositoryIntegrationTest {

    @Autowired TestEntityManager em;

    @Autowired AnalysisJobRepository analysisJobRepository;
    @Autowired BiasAnalysisResultRepository biasAnalysisResultRepository;
    @Autowired ContentSentenceRepository contentSentenceRepository;
    @Autowired SentenceBiasLabelRepository sentenceBiasLabelRepository;
    @Autowired HighlightResultRepository highlightResultRepository;
    @Autowired HighlightSpanRepository highlightSpanRepository;

    // BiasAnalysisResult를 targetId + YOUTUBE_VIDEO로 조회하는 쿼리 검증
    @Test
    void findByTargetIdAndTargetType_returnsResult() {
        AnalysisJob job = analysisJobRepository.save(AnalysisJob.builder()
                .targetId(1L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build());

        biasAnalysisResultRepository.save(BiasAnalysisResult.builder()
                .analysisJob(job)
                .targetId(1L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .overallBiasScore(0.5)
                .build());

        em.flush();
        em.clear();

        Optional<BiasAnalysisResult> found =
                biasAnalysisResultRepository.findByTargetIdAndTargetType(1L, TargetType.YOUTUBE_VIDEO);

        assertThat(found).isPresent();
        assertThat(found.get().getTargetId()).isEqualTo(1L);
        assertThat(found.get().getTargetType()).isEqualTo(TargetType.YOUTUBE_VIDEO);
    }

    // SentenceBiasLabel을 analysisJobId로 조회하는 쿼리 검증
    @Test
    void findAllByAnalysisJobId_returnsLabels() {
        AnalysisJob job = analysisJobRepository.save(AnalysisJob.builder()
                .targetId(1L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build());

        ContentSentence sentence = contentSentenceRepository.save(ContentSentence.builder()
                .targetId(1L)
                .targetType(SentenceTargetType.YOUTUBE_TRANSCRIPT)
                .sentenceOrder(1)
                .sentenceText("편향적인 문장")
                .build());

        sentenceBiasLabelRepository.save(SentenceBiasLabel.builder()
                .analysisJob(job)
                .contentSentence(sentence)
                .labelType(SentenceLabelType.EMOTIONALLY_LOADED)
                .score(0.91)
                .build());

        em.flush();
        em.clear();

        List<SentenceBiasLabel> labels =
                sentenceBiasLabelRepository.findAllByAnalysisJobId(job.getId());

        assertThat(labels).hasSize(1);
        assertThat(labels.get(0).getLabelType()).isEqualTo(SentenceLabelType.EMOTIONALLY_LOADED);
    }

    // HighlightResult를 biasAnalysisResultId로 조회하는 쿼리 검증
    @Test
    void findByBiasAnalysisResultId_returnsHighlightResult() {
        AnalysisJob job = analysisJobRepository.save(AnalysisJob.builder()
                .targetId(1L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build());

        BiasAnalysisResult result = biasAnalysisResultRepository.save(BiasAnalysisResult.builder()
                .analysisJob(job)
                .targetId(1L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .build());

        highlightResultRepository.save(HighlightResult.builder()
                .biasAnalysisResult(result)
                .build());

        em.flush();
        em.clear();

        Optional<HighlightResult> found =
                highlightResultRepository.findByBiasAnalysisResultId(result.getId());

        assertThat(found).isPresent();
    }

    // HighlightSpan을 highlightResultId로 조회하는 쿼리 검증
    @Test
    void findByHighlightResultId_returnsSpans() {
        AnalysisJob job = analysisJobRepository.save(AnalysisJob.builder()
                .targetId(1L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.PENDING)
                .build());

        BiasAnalysisResult result = biasAnalysisResultRepository.save(BiasAnalysisResult.builder()
                .analysisJob(job)
                .targetId(1L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .build());

        ContentSentence sentence = contentSentenceRepository.save(ContentSentence.builder()
                .targetId(1L)
                .targetType(SentenceTargetType.YOUTUBE_TRANSCRIPT)
                .sentenceOrder(1)
                .sentenceText("테스트 문장")
                .build());

        HighlightResult highlightResult = highlightResultRepository.save(HighlightResult.builder()
                .biasAnalysisResult(result)
                .build());

        highlightSpanRepository.save(HighlightSpan.builder()
                .highlightResult(highlightResult)
                .contentSentence(sentence)
                .startOffset(0)
                .endOffset(5)
                .labelType(SentenceLabelType.EMOTIONALLY_LOADED)
                .score(0.91)
                .matchedWord("최악")
                .build());

        em.flush();
        em.clear();

        List<HighlightSpan> spans =
                highlightSpanRepository.findByHighlightResultId(highlightResult.getId());

        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getMatchedWord()).isEqualTo("최악");
        assertThat(spans.get(0).getStartOffset()).isEqualTo(0);
        assertThat(spans.get(0).getEndOffset()).isEqualTo(5);
    }
}
