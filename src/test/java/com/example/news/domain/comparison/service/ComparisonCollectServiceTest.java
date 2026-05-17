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
import com.example.news.domain.issue.repository.IssueClusterItemRepository;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparisonCollectServiceTest {

    @Mock ComparisonProxyService comparisonProxyService;
    @Mock YoutubeSearchService youtubeSearchService;
    @Mock YoutubeVideoRepository youtubeVideoRepository;
    @Mock IssueClusterRepository issueClusterRepository;
    @Mock IssueClusterItemRepository issueClusterItemRepository;
    @Mock IssueGraphSyncService issueGraphSyncService;

    @InjectMocks ComparisonCollectService comparisonCollectService;

    @Test
    void collectMultilingual_returnsSuccess_whenAllCountriesCollected() {
        when(comparisonProxyService.expandMultilingualKeywords("트럼프 대만"))
                .thenReturn(new MultilingualKeywordExpandResponse(
                        "트럼프 대만",
                        new MultilingualKeywordExpandResponse.ExpandedKeywords(
                                List.of("트럼프 대만"),
                                List.of("trump taiwan"),
                                List.of("特朗普 台湾")
                        )
                ));

        YoutubeVideoDto.VideoCard krCard = YoutubeVideoDto.VideoCard.builder().youtubeVideoId("kr1").build();
        YoutubeVideoDto.VideoCard usCard = YoutubeVideoDto.VideoCard.builder().youtubeVideoId("us1").build();
        YoutubeVideoDto.VideoCard cnCard = YoutubeVideoDto.VideoCard.builder().youtubeVideoId("cn1").build();

        when(youtubeSearchService.searchByRegion(anyString(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(List.of(krCard))
                .thenReturn(List.of(usCard))
                .thenReturn(List.of(cnCard));

        when(youtubeVideoRepository.findByYoutubeVideoId("kr1")).thenReturn(Optional.of(
                YoutubeVideo.builder().id(1L).youtubeVideoId("kr1").build()));
        when(youtubeVideoRepository.findByYoutubeVideoId("us1")).thenReturn(Optional.of(
                YoutubeVideo.builder().id(2L).youtubeVideoId("us1").build()));
        when(youtubeVideoRepository.findByYoutubeVideoId("cn1")).thenReturn(Optional.of(
                YoutubeVideo.builder().id(3L).youtubeVideoId("cn1").build()));

        when(issueClusterRepository.save(any())).thenReturn(IssueCluster.builder().id(99L).build());

        CollectMultilingualResponse response = comparisonCollectService.collectMultilingual(
                new CollectMultilingualRequest("트럼프 대만", 10, OffsetDateTime.parse("2026-05-01T00:00:00Z"))
        );

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.collectedCounts().get("KR")).isEqualTo(1);
        assertThat(response.collectedCounts().get("US")).isEqualTo(1);
        assertThat(response.collectedCounts().get("CN")).isEqualTo(1);
        assertThat(response.ingestedVideoIds()).containsExactlyInAnyOrder("kr1", "us1", "cn1");
        verify(issueGraphSyncService).syncIssueNow(any(), any(), any());
    }

    @Test
    void collectMultilingual_returnsPartial_whenOneCountryFails() {
        when(comparisonProxyService.expandMultilingualKeywords("반도체"))
                .thenReturn(new MultilingualKeywordExpandResponse(
                        "반도체",
                        new MultilingualKeywordExpandResponse.ExpandedKeywords(
                                List.of("반도체"),
                                List.of("semiconductor"),
                                List.of("半导体")
                        )
                ));

        when(youtubeSearchService.searchByRegion(anyString(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(List.of(YoutubeVideoDto.VideoCard.builder().youtubeVideoId("kr1").build()))
                .thenThrow(new RuntimeException("US failed"))
                .thenReturn(List.of());

        when(youtubeVideoRepository.findByYoutubeVideoId("kr1")).thenReturn(Optional.of(
                YoutubeVideo.builder().id(1L).youtubeVideoId("kr1").build()));
        when(issueClusterRepository.save(any())).thenReturn(IssueCluster.builder().id(10L).build());

        CollectMultilingualResponse response = comparisonCollectService.collectMultilingual(
                new CollectMultilingualRequest("반도체", 10, null)
        );

        assertThat(response.status()).isEqualTo("partial");
        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).country()).isEqualTo("US");
    }

    @Test
    void collectMultilingual_throwsBadRequest_whenMaxTooLarge() {
        assertThatThrownBy(() -> comparisonCollectService.collectMultilingual(
                new CollectMultilingualRequest("반도체", 21, null)
        )).isInstanceOfSatisfying(ComparisonException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ComparisonErrorCode.INVALID_COMPARISON_REQUEST));
    }

    @Test
    void collectMultilingual_marksPartial_whenEnOrZhSameAsKo() {
        when(comparisonProxyService.expandMultilingualKeywords("트럼프 대만"))
                .thenReturn(new MultilingualKeywordExpandResponse(
                        "트럼프 대만",
                        new MultilingualKeywordExpandResponse.ExpandedKeywords(
                                List.of("트럼프 대만"),
                                List.of("트럼프 대만"),
                                List.of("特朗普台湾")
                        )
                ));

        when(youtubeSearchService.searchByRegion(anyString(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(List.of(
                        YoutubeVideoDto.VideoCard.builder().youtubeVideoId("kr1").build(),
                        YoutubeVideoDto.VideoCard.builder().youtubeVideoId("us1").build(),
                        YoutubeVideoDto.VideoCard.builder().youtubeVideoId("cn1").build()
                ))
                .thenReturn(List.of())
                .thenReturn(List.of());

        when(youtubeVideoRepository.findByYoutubeVideoId(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0, String.class);
            long pk = switch (id) {
                case "kr1" -> 1L;
                case "us1" -> 2L;
                case "cn1" -> 3L;
                default -> 9L;
            };
            return Optional.of(YoutubeVideo.builder().id(pk).youtubeVideoId(id).build());
        });
        when(issueClusterRepository.save(any())).thenReturn(IssueCluster.builder().id(100L).build());

        CollectMultilingualResponse response = comparisonCollectService.collectMultilingual(
                new CollectMultilingualRequest("트럼프 대만", 10, null)
        );

        assertThat(response.status()).isEqualTo("partial");
        assertThat(response.errors().stream().map(CollectMultilingualResponse.CollectError::reason))
                .contains("translation_failed_same_as_ko");
    }
}
