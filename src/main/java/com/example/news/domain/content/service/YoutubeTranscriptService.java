package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeTranscriptDto;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.TranscriptSource;
import com.example.news.domain.content.repository.YoutubeTranscriptRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

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
        // 영상 엔티티 조회
        YoutubeVideo video = youtubeVideoService.getOrFetchVideoEntity(youtubeVideoId);

        // DB 캐시 확인
        Optional<YoutubeTranscript> existing = youtubeTranscriptRepository.findByYoutubeVideo(video);
        if (existing.isPresent()) {
            return YoutubeConverter.toTranscriptDto(existing.get());
        }

        // KR → US 순으로 Python AI Pipeline API 호출
        for (String regionCode : new String[]{"KR", "US"}) {
            AiPipelineTranscriptResponse response = fetchFromAiPipeline(youtubeVideoId, regionCode);
            if (response != null && "success".equals(response.transcriptStatus()) && response.transcript() != null && !response.transcript().isBlank()) {
                String langCode = REGION_TO_LANG.get(regionCode);
                YoutubeTranscript transcript = youtubeTranscriptRepository.save(
                        YoutubeConverter.toTranscriptEntity(video, response.transcript(), TranscriptSource.YOUTUBE_CAPTION, langCode)
                );
                return YoutubeConverter.toTranscriptDto(transcript);
            }
        }

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
        YoutubeVideo video = youtubeVideoService.getOrFetchVideoEntity(youtubeVideoId);

        Optional<YoutubeTranscript> existing = youtubeTranscriptRepository.findByYoutubeVideo(video);
        if (existing.isPresent()) {
            return existing.get();
        }

        for (String regionCode : new String[]{"KR", "US"}) {
            AiPipelineTranscriptResponse response = fetchFromAiPipeline(youtubeVideoId, regionCode);
            if (response != null && "success".equals(response.transcriptStatus()) && response.transcript() != null && !response.transcript().isBlank()) {
                String langCode = REGION_TO_LANG.get(regionCode);
                return youtubeTranscriptRepository.save(
                        YoutubeConverter.toTranscriptEntity(video, response.transcript(), TranscriptSource.YOUTUBE_CAPTION, langCode)
                );
            }
        }

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

            return restTemplate.getForObject(url, AiPipelineTranscriptResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    record AiPipelineTranscriptResponse(
            @JsonProperty("video_id") String videoId,
            String transcript,
            @JsonProperty("transcript_status") String transcriptStatus
    ) {}
}
