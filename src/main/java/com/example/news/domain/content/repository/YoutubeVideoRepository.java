package com.example.news.domain.content.repository;

import com.example.news.domain.content.entity.ContentSource;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.analysis.enums.TargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface YoutubeVideoRepository extends JpaRepository<YoutubeVideo, Long> {
    // 영상 id로 단건 조회
    Optional<YoutubeVideo> findByYoutubeVideoId(String youtubeVideoId);
    // 존재 여부 확인
    boolean existsByYoutubeVideoId(String youtubeVideoId);
    // 채널별 최신순 영상 조회
    List<YoutubeVideo> findByContentSourceOrderByPublishedAtDesc(ContentSource contentSource);

    @Query("""
            select distinct v
            from YoutubeVideo v
            left join BiasAnalysisResult r on r.targetId = v.id and r.targetType = :targetType
            left join BiasAnalysisKeyword k on k.biasAnalysisResult = r
            left join YoutubeVideoKeyword vk on vk.youtubeVideo = v
            left join Keyword sourceKeyword on sourceKeyword = vk.keyword
            where r.id is not null
              and (
                lower(v.title) like lower(concat('%', :keyword, '%'))
                or lower(v.description) like lower(concat('%', :keyword, '%'))
                or lower(v.channelName) like lower(concat('%', :keyword, '%'))
                or lower(k.keywordText) like lower(concat('%', :keyword, '%'))
                or lower(sourceKeyword.keywordName) like lower(concat('%', :keyword, '%'))
                or lower(sourceKeyword.normalizedKeyword) like lower(concat('%', :keyword, '%'))
                or lower(r.summaryText) like lower(concat('%', :keyword, '%'))
                or lower(r.evidenceSummary) like lower(concat('%', :keyword, '%'))
              )
            order by v.collectedAt desc nulls last, v.publishedAt desc nulls last, v.id desc
            """)
    List<YoutubeVideo> searchAnalysisCandidates(
            @Param("keyword") String keyword,
            @Param("targetType") TargetType targetType,
            Pageable pageable
    );

    @Query("""
            select distinct v
            from YoutubeVideo v
            join BiasAnalysisResult r on r.targetId = v.id and r.targetType = :targetType
            order by v.collectedAt desc nulls last, v.publishedAt desc nulls last, v.id desc
            """)
    List<YoutubeVideo> findAnalyzedVideoCandidates(
            @Param("targetType") TargetType targetType,
            Pageable pageable
    );

    @Query("""
            select v
            from YoutubeVideo v
            join BiasAnalysisResult r on r.targetId = v.id and r.targetType = :targetType
            order by r.overallBiasScore desc nulls last, v.id desc
            """)
    List<YoutubeVideo> findHighOverallBiasCandidates(
            @Param("targetType") TargetType targetType,
            Pageable pageable
    );

    @Query("""
            select v
            from YoutubeVideo v
            join BiasAnalysisResult r on r.targetId = v.id and r.targetType = :targetType
            order by r.emotionScore desc nulls last, v.id desc
            """)
    List<YoutubeVideo> findHighEmotionCandidates(
            @Param("targetType") TargetType targetType,
            Pageable pageable
    );

    @Query("""
            select v
            from YoutubeVideo v
            join BiasAnalysisResult r on r.targetId = v.id and r.targetType = :targetType
            order by r.opinionScore desc nulls last, v.id desc
            """)
    List<YoutubeVideo> findHighOpinionCandidates(
            @Param("targetType") TargetType targetType,
            Pageable pageable
    );
}
