package com.example.news.domain.content.service;

import com.example.news.domain.content.converter.YoutubeConverter;
import com.example.news.domain.content.dto.YoutubeTranscriptDto;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.enums.TranscriptSource;
import com.example.news.domain.content.enums.YoutubeErrorCode;
import com.example.news.domain.content.repository.YoutubeTranscriptRepository;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.global.exception.CustomException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeTranscriptService {

    private final YoutubeVideoRepository youtubeVideoRepository;
    private final YoutubeTranscriptRepository youtubeTranscriptRepository;
    private final RestTemplate restTemplate;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private static final String[] PLAYER_RESPONSE_MARKERS = {
            "ytInitialPlayerResponse = ",
            "ytInitialPlayerResponse=",
            "var ytInitialPlayerResponse = "
    };

    @Transactional
    public YoutubeTranscriptDto getTranscript(String youtubeVideoId) {
        YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
                .orElseThrow(() -> new CustomException(YoutubeErrorCode.VIDEO_NOT_FOUND));

        // DB에 이미 있으면 바로 반환
        Optional<YoutubeTranscript> existing = youtubeTranscriptRepository.findByYoutubeVideo(video);
        if (existing.isPresent()) {
            return YoutubeConverter.toTranscriptDto(existing.get());
        }

        // 1단계: 직접 timedtext API 시도 (한국어 → 영어 순)
        for (String lang : List.of("ko", "en")) {
            String text = fetchDirectTimedText(youtubeVideoId, lang);
            if (text != null) {
                YoutubeTranscript transcript = youtubeTranscriptRepository.save(
                        YoutubeConverter.toTranscriptEntity(video, text, TranscriptSource.YOUTUBE_CAPTION, lang)
                );
                return YoutubeConverter.toTranscriptDto(transcript);
            }
        }

        // 2단계: 페이지 파싱으로 captionTracks baseUrl 추출
        List<CaptionTrack> tracks = fetchCaptionTracks(youtubeVideoId);
        if (tracks.isEmpty()) {
            return YoutubeConverter.toUnavailableTranscriptDto(youtubeVideoId);
        }

        // 한국어 우선 → 영어 → 첫 번째 트랙 순으로 선택
        CaptionTrack track = tracks.stream()
                .filter(t -> t.languageCode.startsWith("ko"))
                .findFirst()
                .orElseGet(() -> tracks.stream()
                        .filter(t -> t.languageCode.startsWith("en"))
                        .findFirst()
                        .orElse(tracks.get(0)));

        String transcriptText = fetchTranscriptFromUrl(track.baseUrl);
        if (transcriptText == null) {
            return YoutubeConverter.toUnavailableTranscriptDto(youtubeVideoId);
        }

        YoutubeTranscript transcript = youtubeTranscriptRepository.save(
                YoutubeConverter.toTranscriptEntity(video, transcriptText, TranscriptSource.YOUTUBE_CAPTION, track.languageCode)
        );
        return YoutubeConverter.toTranscriptDto(transcript);
    }

    // 직접 timedtext API 호출 (페이지 파싱 없이 바로 자막 요청)
    private String fetchDirectTimedText(String videoId, String lang) {
        try {
            String url = "https://www.youtube.com/api/timedtext?v=" + videoId
                    + "&lang=" + lang + "&fmt=srv3";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Cookie", "CONSENT=YES+42; SOCS=CAI");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            String body = response.getBody();
            if (body == null || body.isBlank() || body.trim().equals("<?xml version=\"1.0\" encoding=\"utf-8\" ?><transcript/>")) {
                return null;
            }
            String text = parseTimedTextXml(body);
            if (text != null) log.info("Direct timedtext succeeded for video {} lang {}", videoId, lang);
            return text;
        } catch (Exception e) {
            log.debug("Direct timedtext failed for video {} lang {}: {}", videoId, lang, e.getMessage());
            return null;
        }
    }

    // youtube.com/watch 페이지 파싱 → ytInitialPlayerResponse → captionTracks 추출
    private List<CaptionTrack> fetchCaptionTracks(String youtubeVideoId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            headers.set("Cookie", "CONSENT=YES+42; SOCS=CAI");

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://www.youtube.com/watch?v=" + youtubeVideoId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            String html = response.getBody();
            if (html == null) return List.of();

            String json = extractPlayerResponseJson(html);
            if (json == null) return List.of();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode captionTracks = mapper.readTree(json)
                    .path("captions")
                    .path("playerCaptionsTracklistRenderer")
                    .path("captionTracks");

            if (!captionTracks.isArray() || captionTracks.isEmpty()) return List.of();

            List<CaptionTrack> result = new ArrayList<>();
            for (JsonNode node : captionTracks) {
                String langCode = node.path("languageCode").asText();
                String baseUrl = node.path("baseUrl").asText();
                if (!langCode.isEmpty() && !baseUrl.isEmpty()) {
                    result.add(new CaptionTrack(langCode, baseUrl));
                }
            }
            return result;

        } catch (Exception e) {
            log.warn("Failed to fetch caption tracks for video {}: {}", youtubeVideoId, e.getMessage());
            return List.of();
        }
    }

    // 여러 마커 패턴으로 ytInitialPlayerResponse JSON 추출
    private String extractPlayerResponseJson(String html) {
        for (String marker : PLAYER_RESPONSE_MARKERS) {
            int start = html.indexOf(marker);
            if (start == -1) continue;
            start += marker.length();

            int depth = 0;
            for (int i = start; i < html.length(); i++) {
                char c = html.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return html.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    // 자막 트랙 URL로 XML fetch
    private String fetchTranscriptFromUrl(String baseUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            String xml = response.getBody();
            if (xml == null || xml.isBlank()) return null;
            return parseTimedTextXml(xml);
        } catch (Exception e) {
            log.warn("Failed to fetch transcript from URL: {}", e.getMessage());
            return null;
        }
    }

    private String parseTimedTextXml(String xml) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        while (true) {
            int start = xml.indexOf("<text", idx);
            if (start == -1) break;
            int contentStart = xml.indexOf(">", start);
            if (contentStart == -1) break;
            int end = xml.indexOf("</text>", contentStart);
            if (end == -1) break;
            String text = xml.substring(contentStart + 1, end)
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&#39;", "'")
                    .trim();
            if (!text.isEmpty()) {
                sb.append(text).append(" ");
            }
            idx = end + 7;
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private record CaptionTrack(String languageCode, String baseUrl) {}
}
