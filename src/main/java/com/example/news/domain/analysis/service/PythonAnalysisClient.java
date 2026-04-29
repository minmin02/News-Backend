package com.example.news.domain.analysis.service;

import com.example.news.domain.analysis.dto.AnalyzeRawTextRequestDto;
import com.example.news.domain.analysis.dto.AnalyzeRequestDto;
import com.example.news.domain.analysis.dto.BiasAnalysisResultDto;
import com.example.news.domain.analysis.dto.SentenceInputDto;
import com.example.news.domain.content.entity.YoutubeTranscript;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAnalysisClient {

    @Qualifier("pythonWebClient")
    private final WebClient webClient;

    @Value("${python.base-url}")
    private String pythonBaseUrl;

    @Value("${python.client.read-timeout-ms:120000}")
    private long readTimeoutMs;

    @Value("${python.client.retry.max-attempts:1}")
    private int maxAttempts;

    @Value("${python.client.retry.initial-backoff-ms:300}")
    private long initialBackoffMs;

    @Value("${python.client.retry.max-backoff-ms:2000}")
    private long maxBackoffMs;

    public PythonAnalysisCallResult analyzeWithFallback(YoutubeTranscript transcript, Long jobId) {
        Long targetId = transcript.getYoutubeVideo().getId();
        AnalyzeRawTextRequestDto rawRequest = new AnalyzeRawTextRequestDto(
                targetId,
                transcript.getYoutubeVideo().getTitle(),
                transcript.getLanguageCode(),
                transcript.getTranscriptText(),
                "YOUTUBE_VIDEO",
                transcript.getId(),
                transcript.getYoutubeVideo().getCountryCode()
        );

        try {
            BiasAnalysisResultDto rawResult = callWithRetry("/analyze/raw", rawRequest, jobId, targetId, false);
            return new PythonAnalysisCallResult(rawResult, false, "/analyze/raw", List.of());
        } catch (PythonApiException rawFailure) {
            if (!rawFailure.retryable()) {
                throw rawFailure;
            }
            AnalyzeRequestDto fallbackRequest = buildFallbackAnalyzeRequest(transcript, targetId);
            BiasAnalysisResultDto fallbackResult = callWithRetry("/analyze", fallbackRequest, jobId, targetId, true);
            return new PythonAnalysisCallResult(fallbackResult, true, "/analyze", fallbackRequest.sentences());
        }
    }

    private BiasAnalysisResultDto callWithRetry(
            String endpoint,
            Object requestBody,
            Long jobId,
            Long targetId,
            boolean fallbackUsed
    ) {
        int safeMaxAttempts = Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= safeMaxAttempts; attempt++) {
            long startNanos = System.nanoTime();
            try {
                log.info(
                        "python_analysis_request endpoint={} job_id={} target_id={} attempt={} fallback_used={} request_preview={}",
                        endpoint, jobId, targetId, attempt, fallbackUsed, toRequestPreview(requestBody)
                );

                BiasAnalysisResultDto response = invokeEndpoint(endpoint, requestBody);
                long latencyMs = elapsedMillis(startNanos);
                log.info(
                        "python_analysis_response endpoint={} job_id={} target_id={} attempt={} status_code=200 latency_ms={} fallback_used={} final_endpoint={} error_code=-",
                        endpoint, jobId, targetId, attempt, latencyMs, fallbackUsed, endpoint
                );
                return response;
            } catch (Exception ex) {
                long latencyMs = elapsedMillis(startNanos);
                Throwable root = Exceptions.unwrap(ex);
                int statusCode = extractStatusCode(root);
                String errorCode = extractErrorCode(root);
                boolean retryable = isRetryable(root, statusCode);
                Long retryAfterMs = extractRetryAfterMillis(root);

                log.warn(
                        "python_analysis_response endpoint={} job_id={} target_id={} attempt={} status_code={} latency_ms={} fallback_used={} final_endpoint={} error_code={} retryable={}",
                        endpoint, jobId, targetId, attempt, statusCode, latencyMs, fallbackUsed, endpoint, errorCode, retryable
                );

                if (!retryable || attempt == safeMaxAttempts) {
                    throw new PythonApiException(endpoint, statusCode, errorCode, retryable, root);
                }

                long backoffMs = computeBackoffMs(attempt, retryAfterMs);
                sleep(backoffMs);
            }
        }
        throw new PythonApiException(endpoint, 0, "UNKNOWN", false, null);
    }

    protected BiasAnalysisResultDto invokeEndpoint(String endpoint, Object requestBody) {
        return webClient.post()
                .uri(pythonBaseUrl + endpoint)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(BiasAnalysisResultDto.class)
                .timeout(Duration.ofMillis(readTimeoutMs))
                .block();
    }

    private AnalyzeRequestDto buildFallbackAnalyzeRequest(YoutubeTranscript transcript, Long targetId) {
        List<SentenceInputDto> sentences = splitRawTextToSentences(transcript.getTranscriptText());
        return new AnalyzeRequestDto(
                targetId,
                transcript.getYoutubeVideo().getTitle(),
                transcript.getLanguageCode(),
                sentences
        );
    }

    private List<SentenceInputDto> splitRawTextToSentences(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }
        String normalized = rawText.replace("\r\n", "\n").trim();
        String[] tokens = normalized.split("(?<=[.!?。！？])\\s+|\\n+");
        List<String> chunks = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }
        if (chunks.isEmpty()) {
            chunks.add(normalized);
        }
        List<SentenceInputDto> sentences = new ArrayList<>(chunks.size());
        int order = 1;
        for (String chunk : chunks) {
            long stableId = order;
            sentences.add(new SentenceInputDto(stableId, chunk, order, null, null));
            order++;
        }
        return sentences;
    }

    private boolean isRetryable(Throwable throwable, int statusCode) {
        if (statusCode == 429 || statusCode >= 500) {
            return true;
        }
        if (statusCode == 400 || statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode == 422) {
            return false;
        }
        if (statusCode >= 400 && statusCode < 500) {
            return false;
        }
        if (throwable instanceof WebClientRequestException) {
            return true;
        }
        return hasCause(throwable, TimeoutException.class);
    }

    private int extractStatusCode(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            HttpStatusCode statusCode = responseException.getStatusCode();
            return statusCode.value();
        }
        return 0;
    }

    private String extractErrorCode(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            return "HTTP_" + responseException.getStatusCode().value();
        }
        if (throwable instanceof WebClientRequestException) {
            return "NETWORK_ERROR";
        }
        if (hasCause(throwable, TimeoutException.class)) {
            return "TIMEOUT";
        }
        return throwable != null ? throwable.getClass().getSimpleName().toUpperCase(Locale.ROOT) : "UNKNOWN";
    }

    private Long extractRetryAfterMillis(Throwable throwable) {
        if (!(throwable instanceof WebClientResponseException responseException)) {
            return null;
        }
        if (responseException.getStatusCode().value() != 429) {
            return null;
        }
        String retryAfter = responseException.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(retryAfter.trim());
            return Math.max(0L, seconds * 1000L);
        } catch (NumberFormatException ignored) {
            try {
                ZonedDateTime retryTime = ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME);
                long millis = Duration.between(Instant.now(), retryTime.withZoneSameInstant(ZoneOffset.UTC).toInstant()).toMillis();
                return Math.max(0L, millis);
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    private long computeBackoffMs(int attempt, Long retryAfterMs) {
        if (retryAfterMs != null) {
            return retryAfterMs;
        }
        long exponential = initialBackoffMs * (1L << Math.max(0, attempt - 1));
        return Math.min(exponential, maxBackoffMs);
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(Math.max(0L, millis));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new PythonApiException("", 0, "INTERRUPTED", false, interruptedException);
        }
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (type.isInstance(cursor)) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private long elapsedMillis(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private String toRequestPreview(Object requestBody) {
        if (requestBody instanceof AnalyzeRawTextRequestDto raw) {
            int rawLength = raw.rawText() == null ? 0 : raw.rawText().length();
            String preview = raw.rawText() == null ? "" : truncate(raw.rawText(), 120);
            return String.format(
                    "title=%s,raw_text_len=%d,raw_text_preview=%s,transcript_id=%s",
                    truncate(raw.title(), 50), rawLength, preview, raw.transcriptId()
            );
        }
        if (requestBody instanceof AnalyzeRequestDto req) {
            String preview = req.sentences().isEmpty() ? "" : truncate(req.sentences().get(0).sentenceText(), 80);
            return String.format("title=%s,sentences_count=%d,first_sentence_preview=%s",
                    truncate(req.title(), 50), req.sentences().size(), preview);
        }
        return "unsupported_request";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    public record PythonAnalysisCallResult(
            BiasAnalysisResultDto result,
            boolean fallbackUsed,
            String finalEndpoint,
            List<SentenceInputDto> fallbackSentences
    ) {}

    public static class PythonApiException extends RuntimeException {
        private final String endpoint;
        private final int statusCode;
        private final String errorCode;
        private final boolean retryable;

        public PythonApiException(String endpoint, int statusCode, String errorCode, boolean retryable, Throwable cause) {
            super(cause == null ? errorCode : cause.getMessage(), cause);
            this.endpoint = endpoint;
            this.statusCode = statusCode;
            this.errorCode = errorCode;
            this.retryable = retryable;
        }

        public String endpoint() {
            return endpoint;
        }

        public int statusCode() {
            return statusCode;
        }

        public String errorCode() {
            return errorCode;
        }

        public boolean retryable() {
            return retryable;
        }
    }
}
