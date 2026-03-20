package com.example.news.domain.content.repository;

import com.example.news.domain.content.entity.ContentSource;
import com.example.news.domain.content.entity.YoutubeVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface YoutubeVideoRepository extends JpaRepository<YoutubeVideo, Long> {
    // 영상 id로 단건 조회
    Optional<YoutubeVideo> findByYoutubeVideoId(String youtubeVideoId);
    // 존재 여부 확인
    boolean existsByYoutubeVideoId(String youtubeVideoId);
    // 채널별 최신순 영상 조회
    List<YoutubeVideo> findByContentSourceOrderByPublishedAtDesc(ContentSource contentSource);
}
