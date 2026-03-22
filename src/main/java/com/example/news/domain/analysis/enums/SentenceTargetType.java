package com.example.news.domain.analysis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SentenceTargetType {
    YOUTUBE_TRANSCRIPT("youtube_transcript");

    private final String value;
}