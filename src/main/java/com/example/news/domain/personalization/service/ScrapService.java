package com.example.news.domain.personalization.service;

import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.repository.YoutubeVideoRepository;
import com.example.news.domain.personalization.converter.ScrapConverter;
import com.example.news.domain.personalization.dto.ScrapDto;
import com.example.news.domain.personalization.entity.Scrap;
import com.example.news.domain.personalization.enums.ScrapTargetType;
import com.example.news.domain.personalization.exception.DuplicateScrapException;
import com.example.news.domain.personalization.exception.ScrapNotFoundException;
import com.example.news.domain.personalization.repository.ScrapRepository;
import com.example.news.domain.content.exception.VideoNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final YoutubeVideoRepository youtubeVideoRepository;

    @Transactional
    public void save(Long userId, ScrapDto.CreateRequestDto request) {
        YoutubeVideo video = youtubeVideoRepository.findByYoutubeVideoId(request.youtubeVideoId())
                .orElseThrow(VideoNotFoundException::new);

        if (scrapRepository.existsByUserIdAndTargetIdAndTargetType(
                userId, video.getId(), ScrapTargetType.YOUTUBE_VIDEO)) {
            throw new DuplicateScrapException();
        }
        scrapRepository.save(ScrapConverter.toScrap(userId, video.getId()));
    }

    @Transactional(readOnly = true)
    public List<ScrapDto.ScrapResponseDto> getMyScraps(Long userId) {
        List<Scrap> scraps = scrapRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        List<Long> videoIds = scraps.stream()
                .filter(s -> s.getTargetType() == ScrapTargetType.YOUTUBE_VIDEO)
                .map(Scrap::getTargetId)
                .toList();

        Map<Long, YoutubeVideo> videoMap = youtubeVideoRepository.findAllById(videoIds)
                .stream()
                .collect(Collectors.toMap(YoutubeVideo::getId, v -> v));

        return scraps.stream()
                .filter(s -> s.getTargetType() == ScrapTargetType.YOUTUBE_VIDEO
                        && videoMap.containsKey(s.getTargetId()))
                .map(s -> ScrapConverter.toScrapResponseDto(s, videoMap.get(s.getTargetId())))
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long scrapId) {
        Scrap scrap = scrapRepository.findByIdAndUserId(scrapId, userId)
                .orElseThrow(ScrapNotFoundException::new);
        scrapRepository.delete(scrap);
    }
}
