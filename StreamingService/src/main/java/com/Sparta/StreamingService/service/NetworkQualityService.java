package com.Sparta.StreamingService.service;

import com.Sparta.StreamingService.model.BandwidthMeasurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to determine video quality based on network strength
 * Enhanced with actual bandwidth measurement and session tracking
 */
@Service
public class NetworkQualityService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkQualityService.class);
    
    // Store bandwidth measurements per session (sessionId -> BandwidthMeasurement)
    private final Map<String, BandwidthMeasurement> sessionMeasurements = new ConcurrentHashMap<>();
    
    // Session timeout: 30 minutes
    private static final long SESSION_TIMEOUT_MINUTES = 30;

    /**
     * Register or update bandwidth measurement for a session
     * 
     * @param sessionId Client session identifier
     * @param clientIp Client IP address
     * @param bandwidthMbps Measured bandwidth in Mbps
     */
    public void recordBandwidth(String sessionId, String clientIp, double bandwidthMbps) {
        BandwidthMeasurement measurement = sessionMeasurements.computeIfAbsent(
            sessionId, 
            k -> new BandwidthMeasurement(sessionId, clientIp)
        );
        
        measurement.addMeasurement(bandwidthMbps);
        logger.debug("Recorded bandwidth for session {}: {} Mbps", sessionId, bandwidthMbps);
    }

    /**
     * Record manually requested quality for a session
     * 
     * @param sessionId Client session identifier
     * @param quality Requested quality (1080, 720, 480, or null for auto)
     */
    public void recordRequestedQuality(String sessionId, Integer quality) {
        BandwidthMeasurement measurement = sessionMeasurements.get(sessionId);
        if (measurement != null) {
            measurement.setRequestedQuality(quality);
            logger.info("Recorded requested quality for session {}: {}", sessionId, quality);
        } else {
            logger.warn("Session {} not found for quality recording", sessionId);
        }
    }

    /**
     * Get bandwidth measurement for a session
     * 
     * @param sessionId Client session identifier
     * @return BandwidthMeasurement or null if not found
     */
    public BandwidthMeasurement getBandwidthMeasurement(String sessionId) {
        BandwidthMeasurement measurement = sessionMeasurements.get(sessionId);
        
        // Check if session has expired
        if (measurement != null && isSessionExpired(measurement)) {
            sessionMeasurements.remove(sessionId);
            logger.debug("Removed expired session: {}", sessionId);
            return null;
        }
        
        return measurement;
    }

    /**
     * Calculate bandwidth from download metrics
     * 
     * @param bytesDownloaded Bytes downloaded
     * @param downloadTimeMs Time taken in milliseconds
     * @return Bandwidth in Mbps
     */
    public double calculateBandwidth(long bytesDownloaded, long downloadTimeMs) {
        if (downloadTimeMs <= 0) {
            return 0.0;
        }
        
        // Convert bytes to bits, milliseconds to seconds
        double bitsDownloaded = bytesDownloaded * 8.0;
        double timeInSeconds = downloadTimeMs / 1000.0;
        
        // Calculate Mbps
        double bandwidthMbps = (bitsDownloaded / timeInSeconds) / 1_000_000.0;
        
        logger.debug("Calculated bandwidth: {} bytes in {} ms = {} Mbps", 
            bytesDownloaded, downloadTimeMs, bandwidthMbps);
        
        return bandwidthMbps;
    }

    /**
     * Estimate bandwidth from HTTP Range request metrics
     * 
     * @param rangeStart Start byte of range request
     * @param rangeEnd End byte of range request
     * @param downloadTimeMs Time taken to download the range
     * @return Bandwidth in Mbps
     */
    public double estimateBandwidthFromRange(long rangeStart, long rangeEnd, long downloadTimeMs) {
        long bytesDownloaded = rangeEnd - rangeStart + 1;
        return calculateBandwidth(bytesDownloaded, downloadTimeMs);
    }

    /**
     * Determine optimal video quality based on network bandwidth
     * 
     * @param bandwidthMbps Network bandwidth in Mbps
     * @return Recommended quality (1080, 720, or 480)
     */
    public int getOptimalQuality(double bandwidthMbps) {
        logger.debug("Getting optimal quality for bandwidth: {} Mbps", bandwidthMbps);
        if (bandwidthMbps >= 5.0) {
            return 1080; // High bandwidth - serve 1080p
        } else if (bandwidthMbps >= 2.5) {
            return 720;  // Medium bandwidth - serve 720p
        } else if (bandwidthMbps >= 1.0) {
            return 480;  // Low bandwidth - serve 480p
        } else {
            return 480;  // Very low bandwidth - default to 480p
        }
    }

    /**
     * Get quality from request parameter or default based on network
     * 
     * @param requestedQuality Quality requested by client (can be null)
     * @param sessionId Session ID for bandwidth lookup
     * @return Quality to serve
     */
    public int determineQuality(Integer requestedQuality, String sessionId) {
        // If quality is explicitly requested and valid, use it
        if (requestedQuality != null) {
            if (requestedQuality == 1080 || requestedQuality == 720 || requestedQuality == 480) {
                logger.debug("Using explicitly requested quality {} for session {}", requestedQuality, sessionId);
                return requestedQuality;
            }
        }
        
        // Try to get bandwidth from session
        BandwidthMeasurement measurement = getBandwidthMeasurement(sessionId);
        if (measurement != null && measurement.getAverageBandwidth() != null) {
            int recommendedQuality = measurement.getRecommendedQuality();
            logger.debug("Using session-based quality {} for session {}", recommendedQuality, sessionId);
            return recommendedQuality;
        }
        
        // Default to 720p if no network info available
        logger.debug("No bandwidth data available, defaulting to 720p");
        return 720;
    }

    /**
     * Get quality with explicit bandwidth parameter (for backward compatibility)
     * 
     * @param requestedQuality Quality requested by client (can be null)
     * @param estimatedBandwidth Estimated bandwidth in Mbps
     * @return Quality to serve
     */
    public int determineQuality(Integer requestedQuality, Double estimatedBandwidth) {
        if (requestedQuality != null) {
            // Validate requested quality
            if (requestedQuality == 1080 || requestedQuality == 720 || requestedQuality == 480) {
                return requestedQuality;
            }
        }
        
        // If no quality requested or invalid, use network-based selection
        if (estimatedBandwidth != null) {
            return getOptimalQuality(estimatedBandwidth);
        }
        
        // Default to 720p if no network info available
        return 720;
    }

    /**
     * Check if session has expired
     */
    private boolean isSessionExpired(BandwidthMeasurement measurement) {
        if (measurement.getLastUpdated() == null) {
            return true;
        }
        
        long minutesSinceUpdate = ChronoUnit.MINUTES.between(
            measurement.getLastUpdated(), 
            LocalDateTime.now()
        );
        
        return minutesSinceUpdate > SESSION_TIMEOUT_MINUTES;
    }

    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        sessionMeasurements.entrySet().removeIf(entry -> 
            isSessionExpired(entry.getValue())
        );
        logger.debug("Cleaned up expired sessions. Active sessions: {}", sessionMeasurements.size());
    }

    /**
     * Get statistics about bandwidth measurements
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("activeSessions", sessionMeasurements.size());
        
        if (!sessionMeasurements.isEmpty()) {
            double avgBandwidth = sessionMeasurements.values().stream()
                .filter(m -> m.getAverageBandwidth() != null)
                .mapToDouble(m -> m.getAverageBandwidth())
                .average()
                .orElse(0.0);
            
            stats.put("averageBandwidthMbps", avgBandwidth);
        }
        
        return stats;
    }
}
