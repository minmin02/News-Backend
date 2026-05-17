package com.example.news.domain.comparison.service;

import com.example.news.domain.comparison.dto.collect.CollectMultilingualRequest;
import com.example.news.domain.comparison.dto.collect.CollectMultilingualResponse;
import com.example.news.domain.comparison.dto.collect.MultilingualKeywordExpandResponse;
import com.example.news.domain.comparison.exception.ComparisonException;
import com.example.news.domain.comparison.exception.code.ComparisonErrorCode;
import com.example.news.domain.content.dto.YoutubeVideoDto;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.content.service.YoutubeSearchService;
import com.example.news.domain.graph.service.IssueGraphSyncService;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterItem;
import com.example.news.domain.issue.enums.ClusterStatus;
import com.example.news.domain.issue.enums.IssueClusterItemSourceType;
import com.example.news.domain.issue.enums.IssueClusterType;
import com.example.news.domain.issue.repository.IssueClusterItemRepository;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonCollectService {

    private static final List<String> COUNTRIES = List.of("KR", "US", "CN");
    private static final Map<String, String> LANGUAGE_BY_COUNTRY = Map.of(
            "KR", "ko",
            "US", "en",
            "CN", "zh"
    );

    private final ComparisonProxyService comparisonProxyService;
    private final YoutubeSearchService youtubeSearchService;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final IssueClusterRepository issueClusterRepository;
    private final IssueClusterItemRepository issueClusterItemRepository;
    private final IssueGraphSyncService issueGraphSyncService;

    @Transactional
    public CollectMultilingualResponse collectMultilingual(CollectMultilingualRequest request) {
        String keywordKo = request.keywordKo().trim();
        if (keywordKo.isBlank()) {
            throw new ComparisonException(ComparisonErrorCode.INVALID_COMPARISON_REQUEST, "keyword_ko는 비어 있을 수 없습니다.");
        }

        int maxPerLanguage = request.resolvedMaxPerLanguage();
        if (maxPerLanguage > 20) {
            throw new ComparisonException(ComparisonErrorCode.INVALID_COMPARISON_REQUEST, "max_per_language는 20 이하여야 합니다.");
        }

        List<CollectMultilingualResponse.CollectError> errors = new ArrayList<>();
        MultilingualKeywordExpandResponse expanded;
        boolean fallbackApplied = false;
        String fallbackReason = "none";
        try {
            expanded = comparisonProxyService.expandMultilingualKeywords(keywordKo);
        } catch (ComparisonException e) {
            fallbackApplied = true;
            fallbackReason = "python_expand_call_failed";
            expanded = fallbackExpandedKeywords(keywordKo);
            errors.add(CollectMultilingualResponse.CollectError.builder()
                    .country("US")
                    .term(keywordKo)
                    .reason("python_expand_call_failed")
                    .build());
            errors.add(CollectMultilingualResponse.CollectError.builder()
                    .country("CN")
                    .term(keywordKo)
                    .reason("python_expand_call_failed")
                    .build());
        }

        expanded = applyQualityGuard(expanded, keywordKo, errors);
        log.info("pipeline=comparison_collect requested_keyword=\"{}\" fallback_applied={} fallback_reason={} expanded_ko={} expanded_en={} expanded_zh={} event_time={}",
                keywordKo, fallbackApplied, fallbackReason,
                expanded.expandedKeywords().ko(),
                expanded.expandedKeywords().en(),
                expanded.expandedKeywords().zh(),
                OffsetDateTime.now());

        CollectMultilingualResponse.ExpandedKeywords expandedKeywords = CollectMultilingualResponse.ExpandedKeywords.builder()
                .ko(expanded.expandedKeywords().ko())
                .en(expanded.expandedKeywords().en())
                .zh(expanded.expandedKeywords().zh())
                .build();

        Map<String, Integer> collectedCounts = new LinkedHashMap<>();
        COUNTRIES.forEach(country -> collectedCounts.put(country, 0));

        Set<String> ingestedVideoIds = new LinkedHashSet<>();
        Map<Long, YoutubeVideo> ingestedVideoMap = new LinkedHashMap<>();
        List<PendingClusterItem> pendingItems = new ArrayList<>();
        Set<Long> dedupeVideoIdsInCluster = new LinkedHashSet<>();

        for (String country : COUNTRIES) {
            String language = LANGUAGE_BY_COUNTRY.get(country);
            List<String> terms = keywordsForCountry(expanded, country);

            for (String term : terms) {
                try {
                    List<YoutubeVideoDto.VideoCard> cards = youtubeSearchService.searchByRegion(
                            term,
                            country,
                            language,
                            toUtcLocalDateTime(request.publishedAfter()),
                            maxPerLanguage
                    );

                    int addedForCountry = 0;
                    for (YoutubeVideoDto.VideoCard card : cards) {
                        if (card.getYoutubeVideoId() == null || card.getYoutubeVideoId().isBlank()) {
                            continue;
                        }
                        YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(card.getYoutubeVideoId()).orElse(null);
                        if (video == null) {
                            continue;
                        }
                        ingestedVideoIds.add(video.getYoutubeVideoId());
                        ingestedVideoMap.put(video.getId(), video);
                        if (dedupeVideoIdsInCluster.add(video.getId())) {
                            pendingItems.add(new PendingClusterItem(video.getId(), country));
                            addedForCountry++;
                        }
                    }
                    collectedCounts.put(country, collectedCounts.get(country) + addedForCountry);
                } catch (Exception e) {
                    errors.add(CollectMultilingualResponse.CollectError.builder()
                            .country(country)
                            .term(term)
                            .reason(e.getMessage())
                            .build());
                    log.warn("pipeline=comparison_collect country={} term=\"{}\" requested_keyword=\"{}\" reason=\"{}\" event_time={}",
                            country, term, keywordKo, e.getMessage(), OffsetDateTime.now());
                }
            }
        }

        if (!pendingItems.isEmpty()) {
            IssueCluster cluster = issueClusterRepository.save(IssueCluster.builder()
                    .searchKeyword(keywordKo)
                    .normalizedKeyword(keywordKo.toLowerCase())
                    .periodStartDate(resolvePeriodStart(request.publishedAfter()))
                    .periodEndDate(LocalDate.now())
                    .status(ClusterStatus.PENDING)
                    .clusterType(IssueClusterType.CURATION_MANUAL)
                    .build());

            List<IssueClusterItem> clusterItems = pendingItems.stream()
                    .map(item -> IssueClusterItem.builder()
                            .issueCluster(cluster)
                            .youtubeVideoId(item.videoDbId())
                            .countryCode(item.country())
                            .isRepresentative(false)
                            .sourceType(IssueClusterItemSourceType.AUTO)
                            .build())
                    .toList();

            issueClusterItemRepository.saveAll(clusterItems);
            issueGraphSyncService.syncIssueNow(cluster, clusterItems, ingestedVideoMap);
        }

        String status = resolveStatus(collectedCounts, errors);
        return CollectMultilingualResponse.builder()
                .status(status)
                .requestedKeyword(keywordKo)
                .expandedKeywords(expandedKeywords)
                .collectedCounts(collectedCounts)
                .ingestedVideoIds(new ArrayList<>(ingestedVideoIds))
                .errors(errors)
                .build();
    }

    private List<String> keywordsForCountry(MultilingualKeywordExpandResponse expanded, String country) {
        return switch (country) {
            case "KR" -> expanded.expandedKeywords().ko();
            case "US" -> expanded.expandedKeywords().en();
            case "CN" -> expanded.expandedKeywords().zh();
            default -> List.of();
        };
    }

    private MultilingualKeywordExpandResponse fallbackExpandedKeywords(String keywordKo) {
        return new MultilingualKeywordExpandResponse(
                keywordKo,
                new MultilingualKeywordExpandResponse.ExpandedKeywords(
                        List.of(keywordKo),
                        List.of(keywordKo),
                        List.of(keywordKo)
                )
        );
    }

    private MultilingualKeywordExpandResponse applyQualityGuard(MultilingualKeywordExpandResponse expanded,
                                                                String keywordKo,
                                                                List<CollectMultilingualResponse.CollectError> errors) {
        List<String> ko = sanitize(expanded.expandedKeywords().ko(), keywordKo);
        List<String> en = sanitize(expanded.expandedKeywords().en(), keywordKo);
        List<String> zh = sanitize(expanded.expandedKeywords().zh(), keywordKo);

        if (containsSameAsKo(ko, en)) {
            errors.add(CollectMultilingualResponse.CollectError.builder()
                    .country("US")
                    .term(keywordKo)
                    .reason("translation_failed_same_as_ko")
                    .build());
        }
        if (containsSameAsKo(ko, zh)) {
            errors.add(CollectMultilingualResponse.CollectError.builder()
                    .country("CN")
                    .term(keywordKo)
                    .reason("translation_failed_same_as_ko")
                    .build());
        }

        return new MultilingualKeywordExpandResponse(
                expanded.requestedKeyword(),
                new MultilingualKeywordExpandResponse.ExpandedKeywords(ko, en, zh)
        );
    }

    private List<String> sanitize(List<String> keywords, String fallback) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of(fallback);
        }
        List<String> normalized = keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? List.of(fallback) : normalized;
    }

    private boolean containsSameAsKo(List<String> ko, List<String> target) {
        Set<String> koSet = ko.stream().map(this::normalizeString).collect(Collectors.toSet());
        return target.stream()
                .map(this::normalizeString)
                .anyMatch(koSet::contains);
    }

    private String normalizeString(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private LocalDate resolvePeriodStart(OffsetDateTime publishedAfter) {
        if (publishedAfter == null) {
            return LocalDate.now().minusDays(7);
        }
        return publishedAfter.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime publishedAfter) {
        if (publishedAfter == null) {
            return null;
        }
        return LocalDateTime.ofInstant(publishedAfter.toInstant(), ZoneOffset.UTC);
    }

    private String resolveStatus(Map<String, Integer> collectedCounts,
                                 List<CollectMultilingualResponse.CollectError> errors) {
        boolean hasAllCountries = COUNTRIES.stream().allMatch(country -> collectedCounts.getOrDefault(country, 0) > 0);
        int totalCount = collectedCounts.values().stream().collect(Collectors.summingInt(Integer::intValue));

        if (totalCount == 0) {
            return "failed";
        }
        if (hasAllCountries && errors.isEmpty()) {
            return "success";
        }
        return "partial";
    }

    private record PendingClusterItem(Long videoDbId, String country) {}
}
