package com.example.news.domain.content.dto;

import com.example.news.domain.content.enums.TranscriptSource;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class YoutubeTranscriptDto {
    // 자막 응답 객체
    private String youtubeVideoId;
    private String transcriptText;
    private TranscriptSource transcriptSource;
    private String languageCode;
    private Boolean isAvailable;
}
