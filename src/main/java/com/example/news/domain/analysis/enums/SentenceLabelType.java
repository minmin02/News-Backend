package com.example.news.domain.analysis.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SentenceLabelType {
    FACT_LIKE("fact_like"),
    OPINION_LIKE("opinion_like"),
    EMOTIONALLY_LOADED("emotionally_loaded"),
    ANONYMOUS_SOURCE("anonymous_source"),
    SPECULATIVE("speculative");

    private final String value;
}