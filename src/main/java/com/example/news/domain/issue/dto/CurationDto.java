package com.example.news.domain.issue.dto;

import com.example.news.domain.issue.enums.ClusterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class CurationDto {

    public record CreateSetRequest(
            @NotBlank String searchKeyword,
            @NotNull LocalDate periodStartDate,
            @NotNull LocalDate periodEndDate
    ) {}

    public record AddItemRequest(
            @NotNull Long videoId,
            @NotBlank String countryCode
    ) {}

    @Builder
    public record CurationSetResponse(
            Long issueClusterId,
            String searchKeyword,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            ClusterStatus status
    ) {}

    @Builder
    public record CurationItemResponse(
            Long issueClusterItemId,
            Long videoId,
            String countryCode,
            String sourceType
    ) {}

    @Builder
    public record CurationStatusResponse(
            Long issueClusterId,
            ClusterStatus status,
            long totalItems,
            Map<String, Integer> analyzedByCountry,
            Map<String, Integer> remainingByCountry,
            boolean readyForReport,
            List<String> missingCountries
    ) {}
}
