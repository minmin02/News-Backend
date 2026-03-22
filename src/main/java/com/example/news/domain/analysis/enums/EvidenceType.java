package com.example.news.domain.analysis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EvidenceType {
    OPINION("opinion"),
    EMOTION("emotion"),
    ANONYMOUS_SOURCE("anonymous_source"),
    SPECULATION("speculation");

    private final String value;
}