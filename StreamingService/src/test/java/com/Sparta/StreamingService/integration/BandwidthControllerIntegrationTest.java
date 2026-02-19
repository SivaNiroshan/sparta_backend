package com.Sparta.StreamingService.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for BandwidthController HTTP endpoints.
 * No Testcontainers; uses in-memory NetworkQualityService and full Spring Boot context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BandwidthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void reportBandwidth_returns200WithRecommendedQuality() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/bandwidth/report?sessionId=s1&bandwidthMbps=5.0",
                null,
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");
        assertThat(response.getBody()).containsEntry("sessionId", "s1");
        assertThat(response.getBody()).containsKey("recordedBandwidth");
        assertThat(response.getBody()).containsKey("recommendedQuality");
    }

    @Test
    void selectQuality_returns200WhenValid() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/bandwidth/quality/select?sessionId=s1&quality=720",
                null,
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "success");
        assertThat(response.getBody()).containsEntry("selectedQuality", 720);
    }

    @Test
    void selectQuality_returns400WhenInvalid() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/bandwidth/quality/select?sessionId=s1&quality=999",
                null,
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", "error");
    }

    @Test
    void getRecommendedQuality_returnsNoDataWhenUnknownSession() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/bandwidth/quality/unknown-session-xyz",
                Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "no_data");
        assertThat(response.getBody()).containsKey("recommendedQuality");
    }

    @Test
    void getStatistics_returns200WithActiveSessions() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/bandwidth/statistics", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("activeSessions");
    }

    @Test
    void bandwidthTest_returns1MBPayload() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity("/bandwidth/test", byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1024 * 1024);
    }
}
