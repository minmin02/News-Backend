package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContentPreparedEventDto(
        Long contentId,
        Long youtubeTranscriptId,
        // ContentBC에서 title도 보내줘야함, analysis 파이썬에서 제목-본문 갭에서 쓰임
        String title,
        String country,
        String language,
        List<SentenceInputDto> sentences
) {}