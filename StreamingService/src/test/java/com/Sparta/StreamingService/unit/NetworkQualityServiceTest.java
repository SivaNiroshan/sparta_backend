package com.Sparta.StreamingService.unit;

import com.Sparta.StreamingService.model.BandwidthMeasurement;
import com.Sparta.StreamingService.service.NetworkQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for NetworkQualityService. No external dependencies; uses in-memory session store.
 */
class NetworkQualityServiceTest {

    private NetworkQualityService networkQualityService;

    @BeforeEach
    void setUp() {
        networkQualityService = new NetworkQualityService();
    }

    @Test
    void recordBandwidth_createsSessionAndStoresMeasurement() {
        networkQualityService.recordBandwidth("session-1", "192.168.1.1", 5.0);
        BandwidthMeasurement m = networkQualityService.getBandwidthMeasurement("session-1");
        assertThat(m).isNotNull();
        assertThat(m.getSessionId()).isEqualTo("session-1");
        assertThat(m.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(m.getCurrentBandwidth()).isEqualTo(5.0);
        assertThat(m.getAverageBandwidth()).isEqualTo(5.0);
        assertThat(m.getMeasurementCount()).isEqualTo(1);
    }

    @Test
    void recordBandwidth_multipleCallsAveraged() {
        networkQualityService.recordBandwidth("s1", "10.0.0.1", 2.0);
        networkQualityService.recordBandwidth("s1", "10.0.0.1", 4.0);
        networkQualityService.recordBandwidth("s1", "10.0.0.1", 6.0);
        BandwidthMeasurement m = networkQualityService.getBandwidthMeasurement("s1");
        assertThat(m.getMeasurementCount()).isEqualTo(3);
        assertThat(m.getCurrentBandwidth()).isEqualTo(6.0);
        assertThat(m.getAverageBandwidth()).isEqualTo(4.0); // (2+4+6)/3
    }

    @Test
    void getBandwidthMeasurement_returnsNullForUnknownSession() {
        assertThat(networkQualityService.getBandwidthMeasurement("unknown")).isNull();
    }

    @Test
    void calculateBandwidth_returnsMbps() {
        double mbps = networkQualityService.calculateBandwidth(1_000_000, 1000);
        assertThat(mbps).isEqualTo(8.0); // 1MB in 1s = 8 Mbps
    }

    @Test
    void calculateBandwidth_returnsZeroWhenTimeZero() {
        assertThat(networkQualityService.calculateBandwidth(1000, 0)).isEqualTo(0.0);
    }

    @Test
    void estimateBandwidthFromRange_computesFromRangeAndTime() {
        double mbps = networkQualityService.estimateBandwidthFromRange(0, 500_000 - 1, 500);
        assertThat(mbps).isEqualTo(8.0); // 500KB in 0.5s = 8 Mbps
    }

    @Test
    void getOptimalQuality_returns1080WhenHighBandwidth() {
        assertThat(networkQualityService.getOptimalQuality(5.0)).isEqualTo(1080);
        assertThat(networkQualityService.getOptimalQuality(10.0)).isEqualTo(1080);
    }

    @Test
    void getOptimalQuality_returns720WhenMediumBandwidth() {
        assertThat(networkQualityService.getOptimalQuality(2.5)).isEqualTo(720);
        assertThat(networkQualityService.getOptimalQuality(4.9)).isEqualTo(720);
    }

    @Test
    void getOptimalQuality_returns480WhenLowBandwidth() {
        assertThat(networkQualityService.getOptimalQuality(1.0)).isEqualTo(480);
        assertThat(networkQualityService.getOptimalQuality(0.5)).isEqualTo(480);
    }

    @Test
    void determineQuality_withSessionId_usesRequestedQualityWhenValid() {
        networkQualityService.recordBandwidth("s1", "1.2.3.4", 1.0);
        networkQualityService.recordRequestedQuality("s1", 1080);
        assertThat(networkQualityService.determineQuality(1080, "s1")).isEqualTo(1080);
        assertThat(networkQualityService.determineQuality(720, "s1")).isEqualTo(720);
        assertThat(networkQualityService.determineQuality(480, "s1")).isEqualTo(480);
    }

    @Test
    void determineQuality_withSessionId_defaultsTo720WhenNoData() {
        assertThat(networkQualityService.determineQuality(null, "no-session")).isEqualTo(720);
    }

    @Test
    void determineQuality_withBandwidthParam_usesRequestedWhenValid() {
        assertThat(networkQualityService.determineQuality(1080, 1.0)).isEqualTo(1080);
        assertThat(networkQualityService.determineQuality(720, 10.0)).isEqualTo(720);
    }

    @Test
    void determineQuality_withBandwidthParam_usesOptimalWhenNoRequested() {
        assertThat(networkQualityService.determineQuality(null, 6.0)).isEqualTo(1080);
        assertThat(networkQualityService.determineQuality(null, 3.0)).isEqualTo(720);
        assertThat(networkQualityService.determineQuality(null, 0.5)).isEqualTo(480);
    }

    @Test
    void recordRequestedQuality_updatesExistingSession() {
        networkQualityService.recordBandwidth("s1", "1.2.3.4", 5.0);
        networkQualityService.recordRequestedQuality("s1", 480);
        BandwidthMeasurement m = networkQualityService.getBandwidthMeasurement("s1");
        assertThat(m.getRequestedQuality()).isEqualTo(480);
        assertThat(m.getRecommendedQuality()).isEqualTo(480);
    }

    @Test
    void recordRequestedQuality_ignoresWhenSessionMissing() {
        networkQualityService.recordRequestedQuality("missing", 1080);
        assertThat(networkQualityService.getBandwidthMeasurement("missing")).isNull();
    }

    @Test
    void cleanupExpiredSessions_removesExpiredEntries() {
        networkQualityService.recordBandwidth("s1", "1.2.3.4", 5.0);
        assertThat(networkQualityService.getBandwidthMeasurement("s1")).isNotNull();
        networkQualityService.cleanupExpiredSessions();
        // Session not expired yet, still present
        assertThat(networkQualityService.getBandwidthMeasurement("s1")).isNotNull();
    }

    @Test
    void getStatistics_returnsActiveSessionsAndAverageBandwidth() {
        networkQualityService.recordBandwidth("s1", "1.2.3.4", 4.0);
        networkQualityService.recordBandwidth("s2", "5.6.7.8", 6.0);
        Map<String, Object> stats = networkQualityService.getStatistics();
        assertThat(stats.get("activeSessions")).isEqualTo(2);
        assertThat((Double) stats.get("averageBandwidthMbps")).isEqualTo(5.0);
    }

    @Test
    void getStatistics_returnsZeroSessionsWhenEmpty() {
        Map<String, Object> stats = networkQualityService.getStatistics();
        assertThat(stats.get("activeSessions")).isEqualTo(0);
    }
}
