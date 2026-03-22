package com.example.news.domain.analysis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TargetType {
    YOUTUBE_VIDEO("youtube_video"),
    ISSUE_CLUSTER("issue_cluster"),
    ISSUE_CLUSTER_VIDEO("issue_cluster_video");

    private final String value;
}