package com.Sparta.StreamingService.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model to store bandwidth measurements for a client session
 */
@Getter
@Setter
public class BandwidthMeasurement {
    private String sessionId;
    private String clientIp;
    private List<Double> measurements; // Bandwidth measurements in Mbps
    private LocalDateTime lastUpdated;
    private Double averageBandwidth; // Average bandwidth in Mbps
    private Double currentBandwidth; // Most recent measurement in Mbps
    private int measurementCount;
    private Integer requestedQuality; // Manually requested quality (1080, 720, 480, or null for auto)

    public BandwidthMeasurement(String sessionId, String clientIp) {
        this.sessionId = sessionId;
        this.clientIp = clientIp;
        this.measurements = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
        this.measurementCount = 0;
    }

    /**
     * Add a new bandwidth measurement
     */
    public void addMeasurement(double bandwidthMbps) {
        this.measurements.add(bandwidthMbps);
        this.currentBandwidth = bandwidthMbps;
        this.lastUpdated = LocalDateTime.now();
        this.measurementCount++;
        calculateAverage();
    }

    /**
     * Calculate average bandwidth from recent measurements
     */
    private void calculateAverage() {
        if (measurements.isEmpty()) {
            this.averageBandwidth = null;
            return;
        }

        // Use last 10 measurements for average (sliding window)
        int windowSize = Math.min(10, measurements.size());
        List<Double> recentMeasurements = measurements.subList(
            measurements.size() - windowSize, 
            measurements.size()
        );

        double sum = recentMeasurements.stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        this.averageBandwidth = sum / windowSize;
    }

    /**
     * Get the recommended quality based on current bandwidth
     * Returns requestedQuality if set, otherwise calculates based on bandwidth
     */
    public int getRecommendedQuality() {
        // If user manually requested a quality, use it
        if (requestedQuality != null) {
            return requestedQuality;
        }
        
        // Otherwise, calculate based on bandwidth
        double bandwidth = (averageBandwidth != null) ? averageBandwidth : 
                          (currentBandwidth != null) ? currentBandwidth : 0.0;
        
        if (bandwidth >= 5.0) {
            return 1080;
        } else if (bandwidth >= 2.5) {
            return 720;
        } else {
            return 480;
        }
    }
    
    /**
     * Set manually requested quality
     */
    public void setRequestedQuality(Integer quality) {
        this.requestedQuality = quality;
        this.lastUpdated = LocalDateTime.now();
    }
}

