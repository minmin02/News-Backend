package com.example.news.domain.content.repository;

import com.example.news.domain.content.entity.YoutubeComment;
import com.example.news.domain.content.entity.YoutubeVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YoutubeCommentRepository extends JpaRepository<YoutubeComment, Long> {
    // 영상 댓글 좋아요 내림차순
    List<YoutubeComment> findByYoutubeVideoOrderByLikeCountDesc(YoutubeVideo youtubeVideo);
    // 댓글 중복 저장 방지
    boolean existsByExternalCommentId(String externalCommentId);
}
