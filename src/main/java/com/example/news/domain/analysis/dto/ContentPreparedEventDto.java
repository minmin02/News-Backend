package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ContentPreparedEventDto(
        Long contentId,
        Long youtubeTranscriptId,
        String country,
        String language,
        List<SentenceInputDto> sentences
) {}