package com.example.news.domain.issue.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class IssueBiasRequestDto {

    @NotEmpty
    private List<String> youtubeVideoIds;

    @NotEmpty
    private List<String> countries;
}
