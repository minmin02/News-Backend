package com.example.news.domain.comparison.controller;

import com.example.news.domain.comparison.dto.VideoTargetDto;
import com.example.news.domain.comparison.service.ComparisonProxyService;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.content.exception.VideoNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comparison")
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonProxyService comparisonProxyService;
    private final YoutubeVideoRepository youtubeVideoRepository;

    // Python /kg/comparison-home 그대로 전달
    @GetMapping("/home")
    public ResponseEntity<Object> getHome() {
        return ResponseEntity.ok(comparisonProxyService.getHome());
    }

    // Python /kg/search-videos?keyword= 그대로 전달
    @GetMapping("/search")
    public ResponseEntity<Object> searchVideos(@RequestParam String keyword) {
        return ResponseEntity.ok(comparisonProxyService.searchVideos(keyword));
    }

    // Python /kg/videos/{videoId}/comparison-graph 그대로 전달
    @GetMapping("/videos/{videoId}/graph")
    public ResponseEntity<Object> getVideoGraph(@PathVariable String videoId) {
        return ResponseEntity.ok(comparisonProxyService.getVideoGraph(videoId));
    }

    // YouTube video ID → 내부 PK 변환 (그래프 노드 클릭 후 분석 결과 이동용)
    @GetMapping("/videos/{videoId}/target")
    public ResponseEntity<VideoTargetDto> getTarget(@PathVariable String videoId) {
        YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(videoId)
                .orElseThrow(VideoNotFoundException::new);

        return ResponseEntity.ok(VideoTargetDto.builder()
                .videoId(videoId)
                .targetId(video.getId())
                .build());
    }
}
