package com.example.news.domain.analysis.controller;

import com.example.news.domain.analysis.entity.AnalysisJob;
import com.example.news.domain.analysis.enums.JobStatus;
import com.example.news.domain.analysis.enums.JobType;
import com.example.news.domain.analysis.enums.TargetType;
import com.example.news.domain.analysis.service.AnalysisService;
import com.example.news.domain.analysis.service.BiasAnalysisResultService;
import com.example.news.domain.content.entity.YoutubeTranscript;
import com.example.news.domain.content.entity.YoutubeVideo;
import com.example.news.domain.content.service.YoutubeTranscriptService;
import com.example.news.global.config.SecurityConfig;
import com.example.news.global.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AnalysisController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}
        )
)
class AnalysisControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    AnalysisService analysisService;

    @MockBean
    BiasAnalysisResultService biasAnalysisResultService;

    @MockBean
    YoutubeTranscriptService youtubeTranscriptService;

    @Test
    void analyze_returns200_andSuccessStatus() throws Exception {
        YoutubeTranscript transcript = transcript();
        when(youtubeTranscriptService.getOrFetchTranscriptEntity("video-1")).thenReturn(transcript);

        AnalysisJob job = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.SUCCESS)
                .build();
        when(analysisService.createAnalysisJobFromRawText(eq(transcript))).thenReturn(job);

        mockMvc.perform(post("/api/v1/analysis/analyze/{youtubeVideoId}", "video-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.status").value("SUCCESS"));

        verify(analysisService).createAnalysisJobFromRawText(any(YoutubeTranscript.class));
    }

    @Test
    void analyze_returns200_whenJobFailed() throws Exception {
        YoutubeTranscript transcript = transcript();
        when(youtubeTranscriptService.getOrFetchTranscriptEntity("video-1")).thenReturn(transcript);

        AnalysisJob job = AnalysisJob.builder()
                .targetId(10L)
                .targetType(TargetType.YOUTUBE_VIDEO)
                .jobType(JobType.VIDEO_BIAS_ANALYSIS)
                .status(JobStatus.FAILED)
                .build();
        when(analysisService.createAnalysisJobFromRawText(eq(transcript))).thenReturn(job);

        mockMvc.perform(post("/api/v1/analysis/analyze/{youtubeVideoId}", "video-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.status").value("FAILED"));
    }

    private YoutubeTranscript transcript() {
        YoutubeVideo video = YoutubeVideo.builder()
                .id(10L)
                .youtubeVideoId("video-1")
                .title("테스트 영상")
                .countryCode("KR")
                .build();
        return YoutubeTranscript.builder()
                .id(20L)
                .youtubeVideo(video)
                .languageCode("ko")
                .transcriptText("테스트 자막")
                .build();
    }
}
