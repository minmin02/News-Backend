package com.example.news.domain.content.controller;

import com.example.news.domain.content.dto.YoutubeCommentDto;
import com.example.news.domain.content.dto.YoutubeTranscriptDto;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.service.*;
import com.example.news.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/youtube")
@RequiredArgsConstructor
public class YoutubeController {

    private final YoutubeSearchService youtubeSearchService;
    private final YoutubeVideoService youtubeVideoService;
    private final YoutubeCommentService youtubeCommentService;
    private final YoutubeTranscriptService youtubeTranscriptService;
    private final YoutubeChannelService youtubeChannelService;

    // 키워드 검색
    @GetMapping("/search")
    public ApiResponse<List<YoutubeVideoDto.VideoCard>> search(
            @RequestParam String keyword) {
        List<YoutubeVideoDto.VideoCard> result = youtubeSearchService.search(keyword);
        return ApiResponse.ok(result);
    }

    // 추천 방송사 채널 영상 조회
    @GetMapping("/channels/videos")
    public ApiResponse<Map<String, List<YoutubeVideoDto.VideoCard>>> getChannelVideos() {
        Map<String, List<YoutubeVideoDto.VideoCard>> result = youtubeChannelService.getChannelVideos();
        return ApiResponse.ok(result);
    }

    // 키워드 관련 추천 영상 조회 (최근 검색된 영상 반환)
    @GetMapping("/recommendations")
    public ApiResponse<List<YoutubeVideoDto.VideoCard>> getRecommendations(
            @RequestParam(required = false) String keyword) {
        List<YoutubeVideoDto.VideoCard> result = keyword != null
                ? youtubeSearchService.search(keyword)
                : List.of();
        return ApiResponse.ok(result);
    }

    // 영상 단건 상세 조회
    @GetMapping("/{youtubeVideoId}")
    public ApiResponse<YoutubeVideoDto.VideoDetail> getVideo(
            @PathVariable String youtubeVideoId) {
        YoutubeVideoDto.VideoDetail result = youtubeVideoService.getVideo(youtubeVideoId);
        return ApiResponse.ok(result);
    }

    // 댓글 목록 조회
    @GetMapping("/{youtubeVideoId}/comments")
    public ApiResponse<List<YoutubeCommentDto>> getComments(
            @PathVariable String youtubeVideoId) {
        List<YoutubeCommentDto> result = youtubeCommentService.getComments(youtubeVideoId);
        return ApiResponse.ok(result);
    }

    // 자막 조회
    @GetMapping("/{youtubeVideoId}/transcript")
    public ApiResponse<YoutubeTranscriptDto> getTranscript(
            @PathVariable String youtubeVideoId) {
        YoutubeTranscriptDto result = youtubeTranscriptService.getTranscript(youtubeVideoId);
        return ApiResponse.ok(result);
    }

}
