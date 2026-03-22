package com.example.news.domain.analysis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TranscriptSource {
    YOUTUBE_CAPTION("youtube_caption"),
    WHISPER("whisper");

    private final String value;
}