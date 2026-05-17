package com.example.news.domain.comparison.dto.collect;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record CollectMultilingualResponse(
        String status,
        @JsonProperty("requested_keyword") String requestedKeyword,
        @JsonProperty("expanded_keywords") ExpandedKeywords expandedKeywords,
        @JsonProperty("collected_counts") Map<String, Integer> collectedCounts,
        @JsonProperty("ingested_video_ids") List<String> ingestedVideoIds,
        List<CollectError> errors
) {
    @Builder
    public record ExpandedKeywords(
            List<String> ko,
            List<String> en,
            List<String> zh
    ) {}

    @Builder
    public record CollectError(
            String country,
            String term,
            String reason
    ) {}
}
