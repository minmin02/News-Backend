package com.example.news.domain.analysis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BiasKeywordType {
    EMOTION("emotion"),
    FRAME("frame"),
    TOPIC("topic");

    private final String value;
}