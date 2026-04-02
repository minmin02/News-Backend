package com.example.news.domain.issue.service;

import com.example.news.domain.analysis.entity.BiasAnalysisResult;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.repository.BiasAnalysisResultRepository;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.content.service.YoutubeSearchService;
import com.example.news.domain.issue.converter.IssueConverter;
import com.example.news.domain.issue.dto.*;
import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.entity.IssueClusterVideo;
import com.example.news.domain.issue.repository.IssueClusterRepository;
import com.example.news.domain.issue.repository.IssueClusterVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final KeywordTranslationService keywordTranslationService;
    private final YoutubeSearchService youtubeSearchService;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final IssueClusterRepository issueClusterRepository;
    private final IssueClusterVideoRepository issueClusterVideoRepository;
    private final BiasAnalysisResultRepository biasAnalysisResultRepository;

    // 국가별 이슈 영상 검색
    @Transactional
    public IssueSearchResponseDto search(String searchKeyword, String countries, String period) {
        List<String> countryList = IssueConverter.parseCountries(countries);
        LocalDate[] dates = IssueConverter.parsePeriod(period);
        LocalDate startDate = dates[0];
        LocalDate endDate = dates[1];

        // 국가별 키워드 번역
        Map<String, String> translatedKeywords = keywordTranslationService.translate(searchKeyword, countryList);

        // issue_cluster 생성 (검색 세션 한개를 의미함)
        IssueCluster cluster = issueClusterRepository.save(
                IssueCluster.builder()
                        .searchKeyword(searchKeyword)
                        .periodStartDate(startDate)
                        .periodEndDate(endDate)
                        .build()
        );

        // 국가별 YouTube 검색 + issue_cluster_video 저장
        for (String countryCode : countryList) {
            String translatedKeyword = translatedKeywords.get(countryCode);
            String langCode = IssueConverter.getLanguageCode(countryCode);

            var videos = youtubeSearchService.searchByRegion(
                    translatedKeyword, countryCode, langCode, startDate, endDate);

            saveClusterVideos(cluster, countryCode, videos);
        }

        // 저장된 영상 flat 리스트로 반환
        // 이게 뭔소리냐면
        // KR[영상1, 영상2, 영상3]
        // JP[영상4, 영상5, 영상6]
        // 이걸 flat 리스트로 변환해서 [영상1, 영상2, 영상3, 영상4, 영상5, 영상6] 만든다는 소리
        List<IssueClusterVideo> clusterVideos = issueClusterVideoRepository.findByIssueCluster(cluster);
        return IssueConverter.toSearchResponse(cluster, clusterVideos);
    }

    // 국가별 대표 영상 비교 결과 조회 (선택한 영상 ID 직접 지정)
    @Transactional(readOnly = true)
    public IssueComparisonResponseDto comparison(Map<String, String> videoIds) {
        List<IssueComparisonResponseDto.CountryResult> results = new ArrayList<>();

        // Map<국가 코드, youtubeVideoId>를 받아서 각 영상을 db에서 조회
        // 이때 bias_analysis_result 가 없으면 null 반환 (지금은 아마 다 null 반환해야 정상임)
        for (Map.Entry<String, String> entry : videoIds.entrySet()) {
            String countryCode = entry.getKey();
            String youtubeVideoId = entry.getValue();

            YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "YouTube 영상 ID를 찾을 수 없습니다: " + youtubeVideoId));

            BiasAnalysisResult bias = biasAnalysisResultRepository
                    .findByTargetIdAndTargetType(video.getId(), TargetType.YOUTUBE_VIDEO)
                    .orElse(null);

            results.add(IssueConverter.toComparisonCountryResult(countryCode, video, bias));
        }

        return IssueComparisonResponseDto.builder()
                .countries(results)
                .build();
    }

    private void saveClusterVideos(IssueCluster cluster, String countryCode,
                                   List<com.example.news.domain.content.dto.YoutubeVideoDto.VideoCard> videoCards) {
        for (var card : videoCards) {
            youtubeVideoRepository.findByYoutubeVideoId(card.getYoutubeVideoId())
                    .ifPresent(video -> issueClusterVideoRepository.save(
                            IssueClusterVideo.builder()
                                    .issueCluster(cluster)
                                    .youtubeVideo(video)
                                    .countryCode(countryCode)
                                    .isRepresentative(false)
                                    .build()
                    ));
        }
    }
}
