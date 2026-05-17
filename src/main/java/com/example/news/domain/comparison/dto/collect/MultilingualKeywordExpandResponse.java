package com.example.news.domain.comparison.dto.collect;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MultilingualKeywordExpandResponse(
        @JsonProperty("requested_keyword")
        String requestedKeyword,
        @JsonProperty("expanded_keywords")
        ExpandedKeywords expandedKeywords
) {
    public record ExpandedKeywords(
            List<String> ko,
            List<String> en,
            List<String> zh
    ) {}
}
