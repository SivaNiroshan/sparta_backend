package com.Sparta.StreamingService.unit;

import com.Sparta.StreamingService.model.BandwidthMeasurement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BandwidthMeasurement model.
 */
class BandwidthMeasurementTest {

    @Test
    void constructor_setsSessionAndClientIp() {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "192.168.1.1");
        assertThat(m.getSessionId()).isEqualTo("s1");
        assertThat(m.getClientIp()).isEqualTo("192.168.1.1");
        assertThat(m.getMeasurements()).isEmpty();
        assertThat(m.getLastUpdated()).isNotNull();
        assertThat(m.getMeasurementCount()).isZero();
    }

    @Test
    void addMeasurement_incrementsCountAndUpdatesCurrent() {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "1.2.3.4");
        m.addMeasurement(3.0);
        assertThat(m.getMeasurementCount()).isEqualTo(1);
        assertThat(m.getCurrentBandwidth()).isEqualTo(3.0);
        assertThat(m.getAverageBandwidth()).isEqualTo(3.0);
        m.addMeasurement(5.0);
        assertThat(m.getMeasurementCount()).isEqualTo(2);
        assertThat(m.getCurrentBandwidth()).isEqualTo(5.0);
        assertThat(m.getAverageBandwidth()).isEqualTo(4.0);
    }

    @Test
    void getRecommendedQuality_usesRequestedQualityWhenSet() {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "1.2.3.4");
        m.addMeasurement(1.0);
        m.setRequestedQuality(1080);
        assertThat(m.getRecommendedQuality()).isEqualTo(1080);
    }

    @Test
    void getRecommendedQuality_usesBandwidthWhenNoRequested() {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "1.2.3.4");
        m.addMeasurement(6.0);
        assertThat(m.getRecommendedQuality()).isEqualTo(1080);
    }

    @Test
    void getRecommendedQuality_returns480ForLowBandwidth() {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "1.2.3.4");
        m.addMeasurement(0.5);
        assertThat(m.getRecommendedQuality()).isEqualTo(480);
    }

    @Test
    void getRecommendedQuality_defaultsTo480WhenNoBandwidth() {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "1.2.3.4");
        assertThat(m.getRecommendedQuality()).isEqualTo(480);
    }

    @Test
    void setRequestedQuality_updatesLastUpdated() {
        BandwidthMeasurement m = new BandwidthMeasurement("s1", "1.2.3.4");
        m.setRequestedQuality(720);
        assertThat(m.getRequestedQuality()).isEqualTo(720);
        assertThat(m.getLastUpdated()).isNotNull();
    }
}
