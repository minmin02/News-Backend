package com.example.news.domain.issue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ClusterStatus {
    DRAFT("draft"),
    LOCKED("locked"),
    ANALYZING("analyzing"),
    READY_FOR_REPORT("ready_for_report"),
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAIL("fail");

    private final String value;
}
