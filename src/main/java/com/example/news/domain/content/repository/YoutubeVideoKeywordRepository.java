package com.example.news.domain.content.repository;

import com.example.news.domain.content.entity.Keyword;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.entity.YoutubeVideoKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YoutubeVideoKeywordRepository extends JpaRepository<YoutubeVideoKeyword, Long> {
    // 영상 키워드 연결 중복 방지
    boolean existsByYoutubeVideoAndKeyword(YoutubeVideo youtubeVideo, Keyword keyword);
}
