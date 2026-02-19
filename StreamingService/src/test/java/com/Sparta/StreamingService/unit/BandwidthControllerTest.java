package com.Sparta.StreamingService.unit;

import com.Sparta.StreamingService.model.BandwidthMeasurement;
import com.Sparta.StreamingService.service.NetworkQualityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for BandwidthController with mocked NetworkQualityService.
 */
@WebMvcTest(controllers = com.Sparta.StreamingService.controller.BandwidthController.class)
@ActiveProfiles("test")
class BandwidthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NetworkQualityService networkQualityService;

    @Test
    void reportBandwidth_returns200WithRecommendedQuality() throws Exception {
        when(networkQualityService.getOptimalQuality(5.0)).thenReturn(1080);
        mockMvc.perform(post("/bandwidth/report")
                        .param("sessionId", "s1")
                        .param("bandwidthMbps", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.sessionId").value("s1"))
                .andExpect(jsonPath("$.recordedBandwidth").value(5.0))
                .andExpect(jsonPath("$.recommendedQuality").value(1080));
    }

    @Test
    void selectQuality_returns200WhenValid() throws Exception {
        mockMvc.perform(post("/bandwidth/quality/select")
                        .param("sessionId", "s1")
                        .param("quality", "720"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.selectedQuality").value(720));
    }

    @Test
    void selectQuality_returns400WhenInvalidQuality() throws Exception {
        mockMvc.perform(post("/bandwidth/quality/select")
                        .param("sessionId", "s1")
                        .param("quality", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Invalid quality. Must be 1080, 720, 480, or null for auto"));
    }

    @Test
    void getRecommendedQuality_returnsNoDataWhenSessionUnknown() throws Exception {
        when(networkQualityService.getBandwidthMeasurement("unknown")).thenReturn(null);
        mockMvc.perform(get("/bandwidth/quality/unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("no_data"))
                .andExpect(jsonPath("$.recommendedQuality").value(720));
    }

    @Test
    void getRecommendedQuality_returnsDataWhenSessionExists() throws Exception {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "1.2.3.4");
        m.addMeasurement(5.0);
        when(networkQualityService.getBandwidthMeasurement("s1")).thenReturn(m);
        mockMvc.perform(get("/bandwidth/quality/s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.sessionId").value("s1"))
                .andExpect(jsonPath("$.recommendedQuality").exists());
    }

    @Test
    void getStatistics_returnsActiveSessionsAndAverageBandwidth() throws Exception {
        when(networkQualityService.getStatistics()).thenReturn(Map.of(
                "activeSessions", 2,
                "averageBandwidthMbps", 4.5));
        mockMvc.perform(get("/bandwidth/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeSessions").value(2))
                .andExpect(jsonPath("$.averageBandwidthMbps").value(4.5));
    }

    @Test
    void bandwidthTest_returns1MBPayload() throws Exception {
        mockMvc.perform(get("/bandwidth/test"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().longValue("Content-Length", 1024 * 1024));
    }
}
