package com.example.news.domain.analysis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobType {
    TRANSCRIPT_FETCH("transcript_fetch"),
    VIDEO_SUMMARY("video_summary"),
    VIDEO_BIAS_ANALYSIS("video_bias_analysis"),
    ISSUE_COMPARE_ANALYSIS("issue_compare_analysis");

    private final String value;
}