package com.example.news.domain.content.enums;

public enum TranscriptSource {
    // 자막이 어떤 방식으로 수집됐는지 구분하는 enum
    // 유튜브 자막 제공했는지 or AI 음성인식이 필요한지
    YOUTUBE_CAPTION, WHISPER
}
