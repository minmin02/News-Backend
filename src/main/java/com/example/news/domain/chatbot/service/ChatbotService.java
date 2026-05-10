package com.example.news.domain.chatbot.service;

import com.example.news.domain.analysis.dto.AnalysisResultResponse;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.exception.AnalysisException;
import com.example.news.domain.analysis.service.BiasAnalysisResultService;
import com.example.news.domain.chatbot.converter.ChatbotConverter;
import com.example.news.domain.chatbot.dto.ChatbotDto;
import com.example.news.domain.chatbot.entity.ChatMessage;
import com.example.news.domain.chatbot.entity.ChatSession;
import com.example.news.domain.chatbot.enums.MessageRole;
import com.example.news.domain.chatbot.exception.AiPipelineException;
import com.example.news.domain.chatbot.exception.ChatSessionNotFoundException;
import com.example.news.domain.chatbot.exception.ChatSessionUnauthorizedException;
import com.example.news.domain.chatbot.repository.ChatMessageRepository;
import com.example.news.domain.chatbot.repository.ChatSessionRepository;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final YoutubeVideoRepository youtubeVideoRepository;
    private final BiasAnalysisResultService biasAnalysisResultService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${python.base-url}")
    private String pythonBaseUrl;

    private static final String DEFAULT_TITLE = "새 대화";
    private static final String BOT_NAME = "뉴스봇";
    private static final String BOT_PROFILE_IMAGE_URL = "";
    private static final String PENDING_VIDEO_SELECTION = "VIDEO_SELECTION";
    private static final double MIN_INTENT_CONFIDENCE = 0.62;
    private static final Pattern EXACT_NUMBER_SELECTION_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*$");
    private static final Pattern NUMBER_WITH_SUFFIX_SELECTION_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*번\\s*(도|으로|을|를)?\\s*(알려.*|보여.*|선택.*|해줘.*|요약.*)?\\s*[.!?。]*\\s*$");
    private static final List<String> ANALYSIS_REQUEST_TERMS = List.of("분석", "결과", "요약", "편향", "점수");
    private static final List<String> CONCEPT_QUESTION_TERMS = List.of("뭐", "뭔", "뭔가", "뭐야", "뭔가요", "무엇", "무엇인가", "의미", "뜻", "개념", "설명");
    private static final List<String> VIDEO_REFERENCE_TERMS = List.of("영상", "뉴스", "보도", "기사", "채널");
    private static final List<String> DB_DOMAIN_TERMS = List.of("영상", "뉴스", "보도", "기사", "채널", "분석", "결과", "저장", "분석된", "분석한", "관련");
    private static final List<String> DB_LOOKUP_TERMS = List.of("있어", "있는지", "보여", "찾", "조회", "알려", "목록", "리스트", "전체", "어떤");
    private static final List<String> ANALYSIS_LIST_TERMS = List.of("뭐가 있는지", "뭐 있는지", "무엇이 있는지", "목록", "리스트", "전체", "어떤", "저장된", "분석된");
    private static final List<String> RANKING_TERMS = List.of("높은", "높아", "낮은", "낮아", "상위", "가장", "제일", "심한", "큰", "많이");
    private static final List<String> SEARCH_STOP_WORDS = List.of(
            "영상", "분석", "결과", "요약", "요약해줘", "요약해줘요", "알려줘", "알려줘요",
            "설명해줘", "설명해줘요", "관련", "편향", "점수", "다시", "보고싶은데", "보고", "싶은데",
            "이", "그", "저", "좀", "주세요", "해줘", "해줘요", "뭔가요", "뭐가요", "무엇인가요",
            "결과를", "결과가", "결과는", "분석을", "분석이", "분석은", "영상을", "영상이", "영상은",
            "뉴스를", "뉴스가", "뉴스는", "보도를", "보도가", "보도는", "기사를", "기사가", "기사는",
            "저장된", "분석된", "분석한", "알고싶어", "알고싶은데", "알고싶어요", "알려줄래", "알려주세요",
            "보여줘", "보여주세요", "찾아줘", "찾아주세요", "조회해줘", "조회해주세요", "있는지", "있어",
            "거", "것"
    );
    private static final String WELCOME_MESSAGE = "안녕하세요! 뉴스 편향 분석을 도와드리는 뉴스봇입니다. 궁금하신 내용을 아래에서 선택하거나 직접 질문해 주세요.";
    private static final List<String> GUIDE_ITEMS = List.of(
            "편향 점수가 뭔가요?",
            "이 영상 분석 결과 설명해줘",
            "한국 vs 미국 보도 차이 알려줘"
    );

    public ChatbotDto.WelcomeResponseDto getWelcome() {
        return new ChatbotDto.WelcomeResponseDto(
                BOT_NAME,
                BOT_PROFILE_IMAGE_URL,
                WELCOME_MESSAGE,
                GUIDE_ITEMS
        );
    }

    @Transactional
    public ChatbotDto.SessionResponseDto createSession(Long userId, ChatbotDto.CreateSessionRequestDto request) {
        String title = (request.title() != null && !request.title().isBlank())
                ? request.title()
                : DEFAULT_TITLE;

        ChatSession session = chatSessionRepository.save(ChatbotConverter.toChatSession(userId, title));
        return ChatbotConverter.toSessionResponseDto(session);
    }

    @Transactional(readOnly = true)
    public ChatbotDto.SessionListResponseDto getSessions(Long userId) {
        List<ChatSession> sessions = chatSessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);

        List<ChatbotDto.SessionSummaryDto> summaries = sessions.stream()
                .map(session -> {
                    String lastMessage = chatMessageRepository
                            .findTopBySessionIdOrderByCreatedAtDesc(session.getId())
                            .map(ChatMessage::getContent)
                            .orElse(null);
                    return ChatbotConverter.toSessionSummaryDto(session, lastMessage);
                })
                .toList();

        return new ChatbotDto.SessionListResponseDto(summaries);
    }

    @Transactional(readOnly = true)
    public ChatbotDto.MessageListResponseDto getMessages(Long userId, Long sessionId) {
        getOwnedSession(sessionId, userId);

        List<ChatbotDto.MessageResponseDto> messages = chatMessageRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(ChatbotConverter::toMessageResponseDto)
                .toList();

        return new ChatbotDto.MessageListResponseDto(sessionId, messages);
    }

    @Transactional(noRollbackFor = AnalysisException.class)
    public ChatbotDto.SendMessageResponseDto sendMessage(Long userId, Long sessionId, ChatbotDto.SendMessageRequestDto request) {
        ChatSession session = getOwnedSession(sessionId, userId);

        ChatMessage userMessage = saveMessage(sessionId, MessageRole.USER, request.content());

        AnalysisContextResolution contextResolution = resolveAnalysisContext(session, request);

        if (contextResolution.botReply() != null) {
            ChatMessage botMessage = saveMessage(sessionId, MessageRole.BOT, contextResolution.botReply());

            return new ChatbotDto.SendMessageResponseDto(
                    ChatbotConverter.toMessageResponseDto(userMessage),
                    ChatbotConverter.toMessageResponseDto(botMessage)
            );
        }

        List<Map<String, String>> historyPayload = chatMessageRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> Map.of(
                        "role", m.getRole().name().toLowerCase(),
                        "content", m.getContent()
                ))
                .toList();

        String botContent = callAiPipeline(historyPayload, contextResolution.analysisResult());

        ChatMessage botMessage = saveMessage(sessionId, MessageRole.BOT, botContent);

        return new ChatbotDto.SendMessageResponseDto(
                ChatbotConverter.toMessageResponseDto(userMessage),
                ChatbotConverter.toMessageResponseDto(botMessage)
        );
    }

    @Transactional
    public ChatMessage saveMessage(Long sessionId, MessageRole role, String content) {
        return chatMessageRepository.save(ChatbotConverter.toChatMessage(sessionId, role, content));
    }

    @Transactional
    public ChatbotDto.SessionResponseDto updateSessionTitle(Long userId, Long sessionId, ChatbotDto.UpdateSessionTitleRequestDto request) {
        ChatSession session = getOwnedSession(sessionId, userId);
        session.updateTitle(request.title().trim());

        return ChatbotConverter.toSessionResponseDto(session);
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        getOwnedSession(sessionId, userId);

        chatMessageRepository.deleteAllBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

    private ChatSession getOwnedSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(ChatSessionNotFoundException::new);

        if (!session.getUserId().equals(userId)) {
            throw new ChatSessionUnauthorizedException();
        }

        return session;
    }

    private AnalysisContextResolution resolveAnalysisContext(ChatSession session, ChatbotDto.SendMessageRequestDto request) {
        Optional<Integer> selectedIndex = parseSelectedIndex(request.content());

        if (PENDING_VIDEO_SELECTION.equals(session.getPendingContextType()) && selectedIndex.isPresent()) {
            return resolvePendingVideoSelection(session, selectedIndex.get());
        }

        if (PENDING_VIDEO_SELECTION.equals(session.getPendingContextType())) {
            Optional<AnalysisContextResolution> mentionedCandidate = resolvePendingVideoMention(session, request.content());

            if (mentionedCandidate.isPresent()) {
                return mentionedCandidate.get();
            }
        }

        if (PENDING_VIDEO_SELECTION.equals(session.getPendingContextType()) && isAnalysisRankingRequest(request.content())) {
            return resolvePendingVideoRanking(session, request.content());
        }

        Optional<String> contextVideoId = Optional.ofNullable(request.context())
                .map(ChatbotDto.ChatContextDto::videoId)
                .map(String::trim)
                .filter(videoId -> !videoId.isBlank());

        if (contextVideoId.isPresent()) {
            session.clearPendingContext();
            return resolveVideoAnalysis(contextVideoId.get())
                    .map(AnalysisContextResolution::withAnalysis)
                    .orElseGet(() -> AnalysisContextResolution.withBotReply("이 영상의 분석 결과를 아직 찾지 못했어요. 분석이 완료된 뒤 다시 질문해 주세요."));
        }

        if (isConceptOnlyQuestion(request.content())) {
            return AnalysisContextResolution.empty();
        }

        Optional<AnalysisContextResolution> intentResolution = resolveWithIntentParser(session, request);

        if (intentResolution.isPresent()) {
            return intentResolution.get();
        }

        if (isAnalysisListRequest(request.content())) {
            List<VideoCandidate> candidates = findAnalyzedVideoCandidates();

            if (candidates.isEmpty()) {
                session.clearPendingContext();
                return AnalysisContextResolution.withBotReply("아직 확인할 수 있는 영상 분석 결과가 없어요.");
            }

            session.updatePendingContext(PENDING_VIDEO_SELECTION, serializeCandidates(candidates));
            return AnalysisContextResolution.withBotReply(buildCandidateSelectionMessage(candidates));
        }

        if (isAnalysisRankingRequest(request.content())) {
            List<VideoCandidate> candidates = findRankedVideoCandidates(request.content());

            if (candidates.isEmpty()) {
                session.clearPendingContext();
                return AnalysisContextResolution.withBotReply("조건에 맞는 영상 분석 결과를 찾지 못했어요.");
            }

            session.updatePendingContext(PENDING_VIDEO_SELECTION, serializeCandidates(candidates));
            return AnalysisContextResolution.withBotReply(buildCandidateSelectionMessage(candidates));
        }

        if (!shouldSearchAnalysisCandidates(request.content())) {
            return AnalysisContextResolution.empty();
        }

        List<VideoCandidate> candidates = searchVideoCandidates(request.content());

        if (candidates.isEmpty()) {
            session.clearPendingContext();
            return AnalysisContextResolution.withBotReply("관련된 영상 분석 결과를 찾기 어려워요. 영상 제목, 채널명, 주요 키워드를 조금 더 구체적으로 알려주세요.");
        }

        if (candidates.size() == 1) {
            session.clearPendingContext();
            return resolveVideoAnalysis(candidates.get(0).youtubeVideoId())
                    .map(AnalysisContextResolution::withAnalysis)
                    .orElseGet(() -> AnalysisContextResolution.withBotReply("해당 영상은 찾았지만 분석 결과가 아직 없어요. 분석 완료 후 다시 확인해 주세요."));
        }

        session.updatePendingContext(PENDING_VIDEO_SELECTION, serializeCandidates(candidates));
        return AnalysisContextResolution.withBotReply(buildCandidateSelectionMessage(candidates));
    }

    private Optional<AnalysisContextResolution> resolveWithIntentParser(ChatSession session, ChatbotDto.SendMessageRequestDto request) {
        if (!shouldTryIntentParser(session, request)) {
            return Optional.empty();
        }

        ChatIntent intent = callIntentParser(session, request);

        if (intent == null || intent.confidence() < MIN_INTENT_CONFIDENCE || intent.intent() == ChatIntentType.GENERAL_CHAT) {
            return Optional.empty();
        }

        return handleIntent(session, intent);
    }

    private boolean shouldTryIntentParser(ChatSession session, ChatbotDto.SendMessageRequestDto request) {
        String content = request.content();

        if (isConceptOnlyQuestion(content)) {
            return false;
        }

        if (PENDING_VIDEO_SELECTION.equals(session.getPendingContextType())) {
            return true;
        }

        boolean hasDomainTerm = DB_DOMAIN_TERMS.stream().anyMatch(content::contains);
        boolean hasLookupTerm = DB_LOOKUP_TERMS.stream().anyMatch(content::contains);
        boolean hasRankingTerm = RANKING_TERMS.stream().anyMatch(content::contains);

        return hasDomainTerm || hasLookupTerm || hasRankingTerm;
    }

    private Optional<AnalysisContextResolution> handleIntent(ChatSession session, ChatIntent intent) {
        return switch (intent.intent()) {
            case LIST_ANALYZED_VIDEOS -> Optional.of(resolveAnalyzedVideoList(session, Math.max(intent.limit(), 10)));
            case SEARCH_ANALYSIS -> Optional.of(resolveAnalysisSearch(session, intent));
            case SELECT_CANDIDATE -> intent.candidateIndex()
                    .map(index -> Optional.of(resolvePendingVideoSelection(session, index)))
                    .orElse(Optional.empty());
            case RANK_ANALYSIS -> Optional.of(resolveGlobalRanking(session, intent));
            case RANK_CURRENT_CANDIDATES -> Optional.of(resolvePendingVideoRanking(session, intent));
            case GENERAL_CHAT -> Optional.empty();
        };
    }

    private AnalysisContextResolution resolveAnalyzedVideoList(ChatSession session, int limit) {
        List<VideoCandidate> candidates = findAnalyzedVideoCandidates(limit);

        if (candidates.isEmpty()) {
            session.clearPendingContext();
            return AnalysisContextResolution.withBotReply("아직 확인할 수 있는 영상 분석 결과가 없어요.");
        }

        session.updatePendingContext(PENDING_VIDEO_SELECTION, serializeCandidates(candidates));
        return AnalysisContextResolution.withBotReply(buildCandidateSelectionMessage(candidates));
    }

    private AnalysisContextResolution resolveAnalysisSearch(ChatSession session, ChatIntent intent) {
        List<VideoCandidate> candidates = searchVideoCandidates(intent.keywords(), intent.limit());

        if (candidates.isEmpty()) {
            session.clearPendingContext();
            return AnalysisContextResolution.withBotReply("관련된 영상 분석 결과를 찾기 어려워요. 영상 제목, 채널명, 주요 키워드를 조금 더 구체적으로 알려주세요.");
        }

        candidates = rankCandidatesIfNeeded(candidates, intent);

        if (candidates.size() == 1) {
            session.clearPendingContext();
            return resolveVideoAnalysis(candidates.get(0).youtubeVideoId())
                    .map(AnalysisContextResolution::withAnalysis)
                    .orElseGet(() -> AnalysisContextResolution.withBotReply("해당 영상은 찾았지만 분석 결과가 아직 없어요. 분석 완료 후 다시 확인해 주세요."));
        }

        session.updatePendingContext(PENDING_VIDEO_SELECTION, serializeCandidates(candidates));
        return AnalysisContextResolution.withBotReply(buildCandidateSelectionMessage(candidates));
    }

    private AnalysisContextResolution resolveGlobalRanking(ChatSession session, ChatIntent intent) {
        List<VideoCandidate> candidates = findRankedVideoCandidates(intent.sortBy(), intent.limit());

        if (candidates.isEmpty()) {
            session.clearPendingContext();
            return AnalysisContextResolution.withBotReply("조건에 맞는 영상 분석 결과를 찾지 못했어요.");
        }

        session.updatePendingContext(PENDING_VIDEO_SELECTION, serializeCandidates(candidates));
        return AnalysisContextResolution.withBotReply(buildCandidateSelectionMessage(candidates));
    }

    private AnalysisContextResolution resolvePendingVideoSelection(ChatSession session, int selectedIndex) {
        List<VideoCandidate> candidates = deserializeCandidates(session.getPendingContextJson());

        if (selectedIndex < 1 || selectedIndex > candidates.size()) {
            return AnalysisContextResolution.withBotReply("선택할 수 있는 번호가 아니에요. 목록에 있는 번호로 다시 선택해 주세요.");
        }

        VideoCandidate candidate = candidates.get(selectedIndex - 1);

        return resolveVideoAnalysis(candidate.youtubeVideoId())
                .map(AnalysisContextResolution::withAnalysis)
                .orElseGet(() -> AnalysisContextResolution.withBotReply("선택한 영상은 찾았지만 분석 결과가 아직 없어요. 분석 완료 후 다시 확인해 주세요."));
    }

    private Optional<AnalysisContextResolution> resolvePendingVideoMention(ChatSession session, String content) {
        if (!isAnalysisRequest(content)) {
            return Optional.empty();
        }

        List<VideoCandidate> candidates = deserializeCandidates(session.getPendingContextJson());
        String normalizedContent = normalizeForMentionMatch(content);

        VideoCandidate matchedCandidate = candidates.stream()
                .filter(candidate -> {
                    String normalizedTitle = normalizeForMentionMatch(candidate.title());
                    return normalizedTitle.length() >= 6 && normalizedContent.contains(normalizedTitle);
                })
                .findFirst()
                .orElse(null);

        if (matchedCandidate == null) {
            return Optional.empty();
        }

        return Optional.of(resolveVideoAnalysis(matchedCandidate.youtubeVideoId())
                .map(AnalysisContextResolution::withAnalysis)
                .orElseGet(() -> AnalysisContextResolution.withBotReply("언급한 영상은 찾았지만 분석 결과가 아직 없어요. 분석 완료 후 다시 확인해 주세요.")));
    }

    private String normalizeForMentionMatch(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase().replaceAll("[^0-9a-z가-힣]", "");
    }

    private AnalysisContextResolution resolvePendingVideoRanking(ChatSession session, String content) {
        List<VideoCandidate> candidates = deserializeCandidates(session.getPendingContextJson());

        if (candidates.isEmpty()) {
            session.clearPendingContext();
            return AnalysisContextResolution.withBotReply("비교할 후보 목록이 남아 있지 않아요. 먼저 분석 결과 목록이나 관련 영상을 다시 검색해 주세요.");
        }

        RankedCandidate bestCandidate = candidates.stream()
                .map(candidate -> resolveVideoAnalysis(candidate.youtubeVideoId())
                        .map(analysis -> new RankedCandidate(candidate, analysis, pickRankingScoreFromContent(content, analysis)))
                        .orElse(null))
                .filter(candidate -> candidate != null && candidate.score() != null)
                .max((left, right) -> Double.compare(left.score(), right.score()))
                .orElse(null);

        if (bestCandidate == null) {
            return AnalysisContextResolution.withBotReply("후보들의 점수 정보를 확인하지 못했어요. 다시 검색해 주세요.");
        }

        return AnalysisContextResolution.withBotReply(buildRankingAnswer(resolveSortByFromContent(content), bestCandidate));
    }

    private AnalysisContextResolution resolvePendingVideoRanking(ChatSession session, ChatIntent intent) {
        List<VideoCandidate> candidates = deserializeCandidates(session.getPendingContextJson());

        if (candidates.isEmpty()) {
            session.clearPendingContext();
            return AnalysisContextResolution.withBotReply("비교할 후보 목록이 남아 있지 않아요. 먼저 분석 결과 목록이나 관련 영상을 다시 검색해 주세요.");
        }

        RankedCandidate bestCandidate = candidates.stream()
                .map(candidate -> resolveVideoAnalysis(candidate.youtubeVideoId())
                        .map(analysis -> new RankedCandidate(candidate, analysis, pickRankingScore(intent.sortBy(), analysis)))
                        .orElse(null))
                .filter(candidate -> candidate != null && candidate.score() != null)
                .max((left, right) -> Double.compare(left.score(), right.score()))
                .orElse(null);

        if (bestCandidate == null) {
            return AnalysisContextResolution.withBotReply("후보들의 점수 정보를 확인하지 못했어요. 다시 검색해 주세요.");
        }

        return AnalysisContextResolution.withBotReply(buildRankingAnswer(intent.sortBy(), bestCandidate));
    }

    private Optional<AnalysisResultResponse> resolveVideoAnalysis(String youtubeVideoId) {
        return youtubeVideoRepository.findByYoutubeVideoId(youtubeVideoId)
                .flatMap(video -> {
                    try {
                        return Optional.of(biasAnalysisResultService.getAnalysisResult(video.getId()));
                    } catch (AnalysisException e) {
                        log.info("영상 분석 결과 없음: youtubeVideoId={}, reason={}", youtubeVideoId, e.getMessage());
                        return Optional.empty();
                    }
                });
    }

    private List<VideoCandidate> searchVideoCandidates(String content) {
        List<String> keywords = extractSearchKeywords(content);
        return searchVideoCandidates(keywords, 5);
    }

    private List<VideoCandidate> searchVideoCandidates(List<String> keywords, int limit) {
        Map<String, VideoCandidate> candidates = new LinkedHashMap<>();
        int boundedLimit = Math.max(1, Math.min(limit, 10));

        for (String keyword : keywords) {
            youtubeVideoRepository.searchAnalysisCandidates(keyword, TargetType.YOUTUBE_VIDEO, PageRequest.of(0, boundedLimit))
                    .stream()
                    .map(this::toVideoCandidate)
                    .forEach(candidate -> candidates.putIfAbsent(candidate.youtubeVideoId(), candidate));

            if (candidates.size() >= boundedLimit) {
                break;
            }
        }

        return candidates.values().stream().limit(boundedLimit).toList();
    }

    private List<VideoCandidate> findAnalyzedVideoCandidates() {
        return findAnalyzedVideoCandidates(10);
    }

    private List<VideoCandidate> findAnalyzedVideoCandidates(int limit) {
        return youtubeVideoRepository.findAnalyzedVideoCandidates(TargetType.YOUTUBE_VIDEO, PageRequest.of(0, Math.max(1, Math.min(limit, 10))))
                .stream()
                .map(this::toVideoCandidate)
                .toList();
    }

    private List<VideoCandidate> findRankedVideoCandidates(String content) {
        return findRankedVideoCandidates(resolveSortByFromContent(content), 5);
    }

    private List<VideoCandidate> findRankedVideoCandidates(String sortBy, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 10));

        if ("EMOTION_SCORE".equals(sortBy)) {
            return youtubeVideoRepository.findHighEmotionCandidates(TargetType.YOUTUBE_VIDEO, PageRequest.of(0, boundedLimit))
                    .stream()
                    .map(this::toVideoCandidate)
                    .toList();
        }

        if ("OPINION_SCORE".equals(sortBy)) {
            return youtubeVideoRepository.findHighOpinionCandidates(TargetType.YOUTUBE_VIDEO, PageRequest.of(0, boundedLimit))
                    .stream()
                    .map(this::toVideoCandidate)
                    .toList();
        }

        return youtubeVideoRepository.findHighOverallBiasCandidates(TargetType.YOUTUBE_VIDEO, PageRequest.of(0, boundedLimit))
                .stream()
                .map(this::toVideoCandidate)
                .toList();
    }

    private String resolveSortByFromContent(String content) {
        if (content.contains("감정")) {
            return "EMOTION_SCORE";
        }

        if (content.contains("의견") || content.contains("주관")) {
            return "OPINION_SCORE";
        }

        return "OVERALL_BIAS_SCORE";
    }

    private Double pickRankingScoreFromContent(String content, AnalysisResultResponse analysis) {
        return pickRankingScore(resolveSortByFromContent(content), analysis);
    }

    private Double pickRankingScore(String sortBy, AnalysisResultResponse analysis) {
        if ("EMOTION_SCORE".equals(sortBy)) {
            return analysis.emotionScore();
        }

        if ("OPINION_SCORE".equals(sortBy)) {
            return analysis.opinionScore();
        }

        return analysis.overallBiasScore();
    }

    private List<VideoCandidate> rankCandidatesIfNeeded(List<VideoCandidate> candidates, ChatIntent intent) {
        if (intent.sortBy() == null || "RECENT".equals(intent.sortBy())) {
            return candidates;
        }

        return candidates.stream()
                .map(candidate -> resolveVideoAnalysis(candidate.youtubeVideoId())
                        .map(analysis -> new RankedCandidate(candidate, analysis, pickRankingScore(intent.sortBy(), analysis)))
                        .orElse(null))
                .filter(candidate -> candidate != null && candidate.score() != null)
                .sorted((left, right) -> "ASC".equals(intent.sortDirection())
                        ? Double.compare(left.score(), right.score())
                        : Double.compare(right.score(), left.score()))
                .map(RankedCandidate::candidate)
                .limit(intent.limit())
                .toList();
    }

    private String scoreLabelFromSortBy(String sortBy) {
        if ("EMOTION_SCORE".equals(sortBy)) {
            return "감정성 점수";
        }

        if ("OPINION_SCORE".equals(sortBy)) {
            return "의견성 점수";
        }

        return "편향 점수";
    }

    private String buildRankingAnswer(String sortBy, RankedCandidate rankedCandidate) {
        VideoCandidate candidate = rankedCandidate.candidate();
        String scoreLabel = scoreLabelFromSortBy(sortBy);

        StringBuilder builder = new StringBuilder("후보 중에서 ")
                .append(scoreLabel)
                .append("가 가장 높은 영상은\n\n")
                .append(candidate.title());

        if (candidate.channelName() != null && !candidate.channelName().isBlank()) {
            builder.append(" - ").append(candidate.channelName());
        }

        builder.append("\n\n")
                .append(scoreLabel)
                .append(": ")
                .append(String.format("%.2f", rankedCandidate.score()))
                .append(" / 1.0");

        return builder.toString();
    }

    private ChatIntent callIntentParser(ChatSession session, ChatbotDto.SendMessageRequestDto request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", request.content());
            body.put("has_pending_candidates", PENDING_VIDEO_SELECTION.equals(session.getPendingContextType()));
            body.put("has_page_video", Optional.ofNullable(request.context())
                    .map(ChatbotDto.ChatContextDto::videoId)
                    .map(String::trim)
                    .filter(videoId -> !videoId.isBlank())
                    .isPresent());

            Map<?, ?> response = webClient.post()
                    .uri(pythonBaseUrl + "/chatbot/intent")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return null;
            }

            return normalizeIntent(new ChatIntent(
                    parseIntentType(response.get("intent")),
                    parseKeywords(response.get("keywords")),
                    parseSortBy(response.get("sort_by")),
                    parseSortDirection(response.get("sort_direction")),
                    Optional.ofNullable(parseInteger(response.get("candidate_index"))),
                    parseAnswerTask(response.get("answer_task")),
                    parseLimit(response.get("limit")),
                    parseConfidence(response.get("confidence"))
            ));
        } catch (Exception e) {
            log.info("챗봇 intent parser 호출 실패, 규칙 기반 fallback 사용: {}", e.getMessage());
            return null;
        }
    }

    private ChatIntentType parseIntentType(Object value) {
        try {
            return ChatIntentType.valueOf(String.valueOf(value).toUpperCase());
        } catch (Exception e) {
            return ChatIntentType.GENERAL_CHAT;
        }
    }

    private String parseSortBy(Object value) {
        if (value == null) {
            return null;
        }

        String sortBy = String.valueOf(value).toUpperCase();

        return switch (sortBy) {
            case "RECENT", "OVERALL_BIAS_SCORE", "EMOTION_SCORE", "OPINION_SCORE" -> sortBy;
            default -> null;
        };
    }

    private String parseSortDirection(Object value) {
        String sortDirection = value == null ? "DESC" : String.valueOf(value).toUpperCase();
        return "ASC".equals(sortDirection) ? "ASC" : "DESC";
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double parseConfidence(Object value) {
        if (value instanceof Number number) {
            return Math.max(0.0, Math.min(number.doubleValue(), 1.0));
        }

        try {
            return Math.max(0.0, Math.min(Double.parseDouble(String.valueOf(value)), 1.0));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseLimit(Object value) {
        Integer parsed = parseInteger(value);
        int limit = parsed == null ? 5 : parsed;
        return Math.max(1, Math.min(limit, 10));
    }

    private List<String> parseKeywords(Object value) {
        if (!(value instanceof List<?> rawKeywords)) {
            return List.of();
        }

        return rawKeywords.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .distinct()
                .limit(5)
                .toList();
    }

    private String parseAnswerTask(Object value) {
        if (value == null) {
            return null;
        }

        String answerTask = String.valueOf(value).toUpperCase();

        return switch (answerTask) {
            case "SUMMARY", "SCORE", "DETAIL", "COMPARE" -> answerTask;
            default -> null;
        };
    }

    private boolean looksLikeDbLookupIntent(ChatIntent intent) {
        return intent.intent() != ChatIntentType.GENERAL_CHAT && intent.confidence() >= MIN_INTENT_CONFIDENCE;
    }

    private ChatIntent normalizeIntent(ChatIntent rawIntent) {
        if (!looksLikeDbLookupIntent(rawIntent)) {
            return rawIntent;
        }

        if (rawIntent.intent() == ChatIntentType.SEARCH_ANALYSIS && rawIntent.keywords().isEmpty()) {
            return new ChatIntent(ChatIntentType.GENERAL_CHAT, List.of(), null, "DESC", Optional.empty(), null, 5, 0.0);
        }

        return rawIntent;
    }

    private VideoCandidate toVideoCandidate(YoutubeVideo video) {
        return new VideoCandidate(
                video.getYoutubeVideoId(),
                video.getTitle(),
                video.getChannelName()
        );
    }

    private List<String> extractSearchKeywords(String content) {
        String normalizedContent = content
                .replaceAll("(알고\\s*싶\\S*|알려\\s*줘\\S*|설명\\s*해\\S*|요약\\s*해\\S*)", " ")
                .replaceAll("[^0-9A-Za-z가-힣\\s]", " ");
        String[] tokens = normalizedContent.split("\\s+");
        List<String> keywords = new ArrayList<>();

        for (String token : tokens) {
            String normalizedToken = token.trim();

            if (normalizedToken.length() < 2 || SEARCH_STOP_WORDS.contains(normalizedToken)) {
                continue;
            }

            keywords.add(normalizedToken);
        }

        return keywords.stream().distinct().limit(5).toList();
    }

    private boolean isAnalysisRequest(String content) {
        return ANALYSIS_REQUEST_TERMS.stream().anyMatch(content::contains);
    }

    private boolean shouldSearchAnalysisCandidates(String content) {
        boolean hasDomainTerm = DB_DOMAIN_TERMS.stream().anyMatch(content::contains);
        boolean hasLookupTerm = DB_LOOKUP_TERMS.stream().anyMatch(content::contains);

        boolean hasVideoReference = VIDEO_REFERENCE_TERMS.stream().anyMatch(content::contains);
        boolean isConceptQuestion = CONCEPT_QUESTION_TERMS.stream().anyMatch(content::contains);

        if (isConceptQuestion && !hasVideoReference && !hasLookupTerm) {
            return false;
        }

        return (hasDomainTerm && hasLookupTerm) || (isAnalysisRequest(content) && !isConceptQuestion);
    }

    private boolean isConceptOnlyQuestion(String content) {
        boolean isConceptQuestion = CONCEPT_QUESTION_TERMS.stream().anyMatch(content::contains);
        boolean hasVideoReference = VIDEO_REFERENCE_TERMS.stream().anyMatch(content::contains);
        boolean hasListTerm = ANALYSIS_LIST_TERMS.stream().anyMatch(content::contains);
        boolean hasRankingTerm = RANKING_TERMS.stream().anyMatch(content::contains);
        boolean hasAnalysisResultReference = content.contains("분석 결과") || content.contains("분석된")
                || content.contains("분석한") || content.contains("저장된") || content.contains("후보");

        return isConceptQuestion && !hasVideoReference && !hasListTerm && !hasRankingTerm && !hasAnalysisResultReference;
    }

    private boolean isAnalysisListRequest(String content) {
        boolean hasListTerm = ANALYSIS_LIST_TERMS.stream().anyMatch(content::contains);
        boolean hasAnalysisOrStorageTerm = isAnalysisRequest(content) || content.contains("저장") || content.contains("분석된") || content.contains("분석한");

        return hasListTerm && hasAnalysisOrStorageTerm;
    }

    private boolean isAnalysisRankingRequest(String content) {
        boolean hasRankingTerm = RANKING_TERMS.stream().anyMatch(content::contains);
        boolean hasScoreTerm = content.contains("편향") || content.contains("점수") || content.contains("감정") || content.contains("의견") || content.contains("주관");

        return hasRankingTerm && hasScoreTerm;
    }

    private Optional<Integer> parseSelectedIndex(String content) {
        Matcher matcher = EXACT_NUMBER_SELECTION_PATTERN.matcher(content);

        if (matcher.matches()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }

        matcher = NUMBER_WITH_SUFFIX_SELECTION_PATTERN.matcher(content);

        if (matcher.matches()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }

        return Optional.empty();
    }

    private String buildCandidateSelectionMessage(List<VideoCandidate> candidates) {
        StringBuilder builder = new StringBuilder("관련 영상이 여러 개 있어요. 어떤 영상의 분석 결과를 볼까요?\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            VideoCandidate candidate = candidates.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(candidate.title());

            if (candidate.channelName() != null && !candidate.channelName().isBlank()) {
                builder.append(" - ").append(candidate.channelName());
            }

            builder.append("\n");
        }

        builder.append("\n번호로 선택해 주세요.");
        return builder.toString();
    }

    private String serializeCandidates(List<VideoCandidate> candidates) {
        try {
            return objectMapper.writeValueAsString(candidates);
        } catch (JsonProcessingException e) {
            log.warn("영상 후보 직렬화 실패: {}", e.getMessage());
            return "[]";
        }
    }

    private List<VideoCandidate> deserializeCandidates(String pendingContextJson) {
        if (pendingContextJson == null || pendingContextJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(pendingContextJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("영상 후보 역직렬화 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String callAiPipeline(List<Map<String, String>> history, AnalysisResultResponse analysisResult) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("messages", history);

            if (analysisResult != null) {
                body.put("analysis_context", objectMapper.convertValue(analysisResult, Map.class));
            }

            Map<?, ?> response = webClient.post()
                    .uri(pythonBaseUrl + "/chatbot/chat")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("reply")) {
                throw new AiPipelineException();
            }

            return (String) response.get("reply");
        } catch (AiPipelineException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI pipeline 호출 실패: {}", e.getMessage());
            throw new AiPipelineException();
        }
    }

    private record AnalysisContextResolution(
            AnalysisResultResponse analysisResult,
            String botReply
    ) {
        private static AnalysisContextResolution empty() {
            return new AnalysisContextResolution(null, null);
        }

        private static AnalysisContextResolution withAnalysis(AnalysisResultResponse analysisResult) {
            return new AnalysisContextResolution(analysisResult, null);
        }

        private static AnalysisContextResolution withBotReply(String botReply) {
            return new AnalysisContextResolution(null, botReply);
        }
    }

    private record VideoCandidate(
            String youtubeVideoId,
            String title,
            String channelName
    ) {}

    private record RankedCandidate(
            VideoCandidate candidate,
            AnalysisResultResponse analysis,
            Double score
    ) {}

    private enum ChatIntentType {
        GENERAL_CHAT,
        LIST_ANALYZED_VIDEOS,
        SEARCH_ANALYSIS,
        SELECT_CANDIDATE,
        RANK_ANALYSIS,
        RANK_CURRENT_CANDIDATES
    }

    private record ChatIntent(
            ChatIntentType intent,
            List<String> keywords,
            String sortBy,
            String sortDirection,
            Optional<Integer> candidateIndex,
            String answerTask,
            int limit,
            double confidence
    ) {}
}
