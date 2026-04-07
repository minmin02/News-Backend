package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeTranscriptDto;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.TranscriptSource;
import com.example.news.domain.content.repository.YoutubeTranscriptRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeTranscriptService {

    private final YoutubeTranscriptRepository youtubeTranscriptRepository;
    private final RestTemplate restTemplate;
    private final YoutubeVideoService youtubeVideoService;

    @Value("${ai-pipeline.base-url}")
    private String aiPipelineBaseUrl;

    private static final Map<String, String> REGION_TO_LANG = Map.of(
            "KR", "ko",
            "US", "en",
            "JP", "ja"
    );

    @Transactional
    public YoutubeTranscriptDto getTranscript(String youtubeVideoId) {
        log.info("[Transcript] 요청 - videoId={}", youtubeVideoId);

        // 영상 엔티티 조회
        YoutubeVideo video = youtubeVideoService.getOrFetchVideoEntity(youtubeVideoId);

        // DB 캐시 확인
        Optional<YoutubeTranscript> existing = youtubeTranscriptRepository.findByYoutubeVideo(video);
        if (existing.isPresent()) {
            log.info("[Transcript] DB 캐시 히트 - videoId={}, transcriptId={}", youtubeVideoId, existing.get().getId());
            return YoutubeConverter.toTranscriptDto(existing.get());
        }
        log.debug("[Transcript] DB 캐시 없음 - videoId={}", youtubeVideoId);

        // KR → US 순으로 Python AI Pipeline API 호출
        for (String regionCode : new String[]{"KR", "US"}) {
            log.debug("[Transcript] AI Pipeline 호출 시도 - videoId={}, region={}", youtubeVideoId, regionCode);
            AiPipelineTranscriptResponse response = fetchFromAiPipeline(youtubeVideoId, regionCode);
            if (response != null && "success".equals(response.transcriptStatus()) && response.transcript() != null && !response.transcript().isBlank()) {
                String langCode = REGION_TO_LANG.get(regionCode);
                log.info("[Transcript] AI Pipeline 성공 - videoId={}, region={}, lang={}, transcriptLength={}",
                        youtubeVideoId, regionCode, langCode, response.transcript().length());
                YoutubeTranscript transcript = youtubeTranscriptRepository.save(
                        YoutubeConverter.toTranscriptEntity(video, response.transcript(), TranscriptSource.YOUTUBE_CAPTION, langCode)
                );
                return YoutubeConverter.toTranscriptDto(transcript);
            } else {
                log.warn("[Transcript] AI Pipeline 실패 - videoId={}, region={}, response={}", youtubeVideoId, regionCode, response);
            }
        }

        log.warn("[Transcript] 자막 없음 - videoId={}", youtubeVideoId);
        return YoutubeConverter.toUnavailableTranscriptDto(youtubeVideoId);
    }

    /**
     * transcript entity를 반환한다. 없으면 Python AI Pipeline에서 fetch 후 저장.
     * DB id, transcriptText 등 entity 전체가 필요한 경우 사용.
     *
     * @return YoutubeTranscript entity, 자막 없으면 null
     */
    @Transactional
    public YoutubeTranscript getOrFetchTranscriptEntity(String youtubeVideoId) {
        log.info("[TranscriptEntity] 요청 - videoId={}", youtubeVideoId);

        YoutubeVideo video = youtubeVideoService.getOrFetchVideoEntity(youtubeVideoId);

        Optional<YoutubeTranscript> existing = youtubeTranscriptRepository.findByYoutubeVideo(video);
        if (existing.isPresent()) {
            log.info("[TranscriptEntity] DB 캐시 히트 - videoId={}, transcriptId={}", youtubeVideoId, existing.get().getId());
            return existing.get();
        }
        log.debug("[TranscriptEntity] DB 캐시 없음 - videoId={}", youtubeVideoId);

        for (String regionCode : new String[]{"KR", "US"}) {
            log.debug("[TranscriptEntity] AI Pipeline 호출 시도 - videoId={}, region={}", youtubeVideoId, regionCode);
            AiPipelineTranscriptResponse response = fetchFromAiPipeline(youtubeVideoId, regionCode);
            if (response != null && "success".equals(response.transcriptStatus()) && response.transcript() != null && !response.transcript().isBlank()) {
                String langCode = REGION_TO_LANG.get(regionCode);
                log.info("[TranscriptEntity] AI Pipeline 성공 - videoId={}, region={}, lang={}, transcriptLength={}",
                        youtubeVideoId, regionCode, langCode, response.transcript().length());
                return youtubeTranscriptRepository.save(
                        YoutubeConverter.toTranscriptEntity(video, response.transcript(), TranscriptSource.YOUTUBE_CAPTION, langCode)
                );
            } else {
                log.warn("[TranscriptEntity] AI Pipeline 실패 - videoId={}, region={}, response={}", youtubeVideoId, regionCode, response);
            }
        }

        log.warn("[TranscriptEntity] 자막 없음, null 반환 - videoId={}", youtubeVideoId);
        return null;
    }

    // Python api 호출
    private AiPipelineTranscriptResponse fetchFromAiPipeline(String videoId, String regionCode) {
        try {
            // URL 조립
            String url = UriComponentsBuilder.fromHttpUrl(aiPipelineBaseUrl)
                    .path("/content/transcript")
                    .queryParam("video_id", videoId)
                    .queryParam("region_code", regionCode)
                    .toUriString();
            log.debug("[Transcript] AI Pipeline URL - {}", url);

            return restTemplate.getForObject(url, AiPipelineTranscriptResponse.class);
        } catch (Exception e) {
            log.warn("[Transcript] AI Pipeline API 호출 예외 - videoId={}, region={}, error={}", videoId, regionCode, e.getMessage());
            return null;
        }
    }

    private record AiPipelineTranscriptResponse(
            @JsonProperty("video_id") String videoId,
            String transcript,
            @JsonProperty("transcript_status") String transcriptStatus
    ) {}
}
