package com.example.news.domain.analysis.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BiasAnalysisResultDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSnakeCaseFields_emotionScore_andSentenceLabels() throws Exception {
        String json = """
                {
                  "target_id": 10,
                  "overall_bias_score": 0.5,
                  "opinion_score": 0.4,
                  "emotion_score": 0.3,
                  "anonymous_source_score": 0.2,
                  "summary_text": "summary",
                  "perspective_summary": "perspective",
                  "evidence_summary": "evidence",
                  "tone_label": "neutral",
                  "subjectivity_score": 0.7,
                  "score_evidence": "score",
                  "bias_type_scores": {"EMOTIONAL": 0.3},
                  "sentence_labels": [
                    {
                      "content_sentence_id": 1,
                      "label_type": "EMOTIONALLY_LOADED",
                      "score": 0.91,
                      "highlight_color": "YELLOW",
                      "evidence_keyword": "최악",
                      "start_offset": 1,
                      "end_offset": 3,
                      "matched_word": "최악"
                    }
                  ]
                }
                """;

        BiasAnalysisResultDto dto = objectMapper.readValue(json, BiasAnalysisResultDto.class);

        assertThat(dto.emotionScore()).isEqualTo(0.3);
        assertThat(dto.sentenceLabels()).hasSize(1);
        assertThat(dto.sentenceLabels().get(0).contentSentenceId()).isEqualTo(1L);
        assertThat(dto.sentenceLabels().get(0).labelType()).isEqualTo("EMOTIONALLY_LOADED");
    }
}
