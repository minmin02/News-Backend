package com.example.news.domain.issue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClusterStatus {
    PENDING("pending"),
    COMPLETED("completed"),
    FAIL("fail");

    private final String value;
}