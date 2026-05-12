package com.example.news.domain.comparison.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoTargetDto {
    private String videoId;
    private Long targetId;
}
