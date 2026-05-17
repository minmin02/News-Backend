package com.example.news.domain.comparison.dto.collect;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record CollectMultilingualRequest(
        @JsonProperty("keyword_ko")
        @NotBlank(message = "keyword_ko는 필수입니다.")
        String keywordKo,

        @JsonProperty("max_per_language")
        @Max(value = 20, message = "max_per_language는 20 이하여야 합니다.")
        Integer maxPerLanguage,

        @JsonProperty("published_after")
        OffsetDateTime publishedAfter
) {
    public int resolvedMaxPerLanguage() {
        if (maxPerLanguage == null) {
            return 10;
        }
        return Math.max(1, maxPerLanguage);
    }
}
