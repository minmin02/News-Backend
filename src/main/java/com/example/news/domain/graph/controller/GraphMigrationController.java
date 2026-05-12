package com.example.news.domain.graph.controller;

import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.graph.service.VideoGraphSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/graph")
@RequiredArgsConstructor
public class GraphMigrationController {

    private final YoutubeVideoRepository youtubeVideoRepository;
    private final VideoGraphSyncService videoGraphSyncService;

    // 기존 youtube_video 테이블 → Neo4j 전체 이관 (최초 1회 실행)
    // JWT 인증 필요 (SecurityConfig 기본 설정)
    @PostMapping("/migrate/videos")
    public ResponseEntity<Map<String, Object>> migrateVideos() {
        int pageSize = 100;
        int page = 0;
        int total = 0;

        Page<YoutubeVideo> result;
        do {
            result = youtubeVideoRepository.findAll(PageRequest.of(page++, pageSize));
            for (YoutubeVideo video : result.getContent()) {
                videoGraphSyncService.syncVideo(video);
                total++;
            }
        } while (result.hasNext());

        return ResponseEntity.ok(Map.of("total", total, "message", "완료"));
    }
}
